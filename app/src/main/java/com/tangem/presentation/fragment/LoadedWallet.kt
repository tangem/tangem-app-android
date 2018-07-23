package com.tangem.presentation.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.zxing.WriterException
import com.tangem.data.network.Server
import com.tangem.data.network.VolleyHelper
import com.tangem.data.network.request.ElectrumRequest
import com.tangem.data.network.request.ExchangeRequest
import com.tangem.data.network.request.InfuraRequest
import com.tangem.data.network.request.VerificationServerProtocol
import com.tangem.data.network.task.VerificationServerTask
import com.tangem.data.network.task.loaded_wallet.ETHRequestTask
import com.tangem.data.network.task.loaded_wallet.RateInfoTask
import com.tangem.data.network.task.loaded_wallet.UpdateWalletInfoTask
import com.tangem.data.nfc.VerifyCardTask
import com.tangem.domain.cardReader.CardProtocol
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.*
import com.tangem.presentation.activity.*
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.PINSwapWarningDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.util.Util
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fr_loaded_wallet.*
import java.util.*

class LoadedWallet : Fragment(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        val TAG: String = LoadedWallet::class.java.simpleName

        private const val REQUEST_CODE_SEND_PAYMENT = 1
        private const val REQUEST_CODE_VERIFY_CARD = 4
        private const val REQUEST_CODE_PURGE = 2
        private const val REQUEST_CODE_REQUEST_PIN2_FOR_PURGE = 3
        private const val REQUEST_CODE_ENTER_NEW_PIN = 5
        private const val REQUEST_CODE_ENTER_NEW_PIN2 = 6
        private const val REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN = 7
        private const val REQUEST_CODE_SWAP_PIN = 8

        private const val URL_CARD_VALIDATE = Server.API.Method.CARD_VALIDATE
    }

    private var nfcManager: NfcManager? = null
    var mCard: TangemCard? = null
    private var lastTag: Tag? = null

    var srlLoadedWallet: SwipeRefreshLayout? = null

    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0
    private var timerHideErrorAndMessage: Timer? = null
    private var newPIN = ""
    private var newPIN2 = ""
    private var mCardProtocol: CardProtocol? = null

    private var onlineVerifyTask: OnlineVerifyTask? = null

    private inner class OnlineVerifyTask : VerificationServerTask() {

        override fun onPostExecute(requests: List<VerificationServerProtocol.Request>) {
            super.onPostExecute(requests)
            onlineVerifyTask = null

            for (request in requests) {
                if (request.error == null) {
                    val answer = request.answer as VerificationServerProtocol.Verify.Answer
                    if (answer.error == null) {
                        mCard!!.isOnlineVerified = answer.results[0].passed
                    } else {
                        mCard!!.isOnlineVerified = null
                    }
                } else {
                    mCard!!.isOnlineVerified = null
                }
            }
            srlLoadedWallet!!.isRefreshing = false
        }
    }

    private fun requestCardValidate() {
        val params = HashMap<String, String>()
        params["CID"] = Util.bytesToHex(mCard!!.cid)
        params["publicKey"] = Util.bytesToHex(mCard!!.cardPublicKey)
        VolleyHelper().doRequestString(context!!, URL_CARD_VALIDATE, params)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcManager = NfcManager(activity, this)

        mCard = TangemCard(activity!!.intent.getStringExtra(TangemCard.EXTRA_CARD))
        mCard!!.LoadFromBundle(activity!!.intent.extras.getBundle(TangemCard.EXTRA_CARD))

        lastTag = activity!!.intent.getParcelableExtra(Main.EXTRA_LAST_DISCOVERED_TAG)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_loaded_wallet, container, false)

        srlLoadedWallet = v.findViewById(R.id.srlLoadedWallet)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mCard!!.blockchain == Blockchain.Token)
            tvBalance.setSingleLine(false)

        ivTangemCard.setImageResource(mCard!!.cardImageResource)

        val engine = CoinEngineFactory.Create(mCard!!.blockchain)
        val visibleFlag = engine?.InOutPutVisible() ?: true
        val visibleIOPuts = if (visibleFlag) View.VISIBLE else View.GONE

        tvInputs.visibility = visibleIOPuts

        try {
            ivQR.setImageBitmap(UtilHelper.generateQrCode(engine!!.getShareWalletURI(mCard).toString()))
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        updateViews()

        if (!mCard!!.hasBalanceInfo()) {
            srlLoadedWallet!!.isRefreshing = true
            srlLoadedWallet!!.postDelayed({ this.refresh() }, 1000)
        }

        if ((mCard!!.isOnlineVerified == null || !mCard!!.isOnlineVerified) && onlineVerifyTask == null) {
            onlineVerifyTask = OnlineVerifyTask()
            onlineVerifyTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, VerificationServerProtocol.Verify.prepare(mCard))
        }

        startVerify(lastTag)

        tvWallet.text = mCard!!.wallet

        // set listeners
        srlLoadedWallet!!.setOnRefreshListener { this.refresh() }

        ivLookup.setOnClickListener {
            if (mCard!!.hasBalanceInfo()) {
                val engineClick = CoinEngineFactory.Create(mCard!!.blockchain)
                val browserIntent = Intent(Intent.ACTION_VIEW, engineClick!!.getShareWalletURIExplorer(mCard))
                startActivity(browserIntent)
            }
        }

        ivCopy.setOnClickListener { doShareWallet(false) }

        tvWallet.setOnClickListener { doShareWallet(false) }

        ivQR.setOnClickListener { doShareWallet(true) }

        btnLoad.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, CoinEngineFactory.Create(mCard!!.blockchain)!!.getShareWalletURI(mCard))
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_compatible_wallet, Toast.LENGTH_LONG).show()
            }
        }

        fabInfo.setOnClickListener {
            if (mCardProtocol != null)
                openVerifyCard(mCardProtocol!!)
            else
                Toast.makeText(context, R.string.need_attach_card_again, Toast.LENGTH_LONG).show()
        }

        fabNFC.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }

        btnExtract.setOnClickListener {
            if (!mCard!!.hasBalanceInfo()) {

            } else if (!engine!!.IsBalanceNotZero(mCard))
                Toast.makeText(context, R.string.wallet_empty, Toast.LENGTH_LONG).show()
            else if (!engine.IsBalanceAlterNotZero(mCard))
                Toast.makeText(context, R.string.not_enough_funds, Toast.LENGTH_LONG).show()
            else if (engine.AwaitingConfirmation(mCard))
                Toast.makeText(context, R.string.please_wait_while_previous, Toast.LENGTH_LONG).show()
            else if (!engine.CheckUnspentTransaction(mCard))
                Toast.makeText(context, R.string.please_wait_for_confirmation, Toast.LENGTH_LONG).show()
            else if (mCard!!.remainingSignatures == 0)
                Toast.makeText(context, R.string.card_hasn_t_remaining_signature, Toast.LENGTH_LONG).show()

            val intent = Intent(context, PreparePaymentActivity::class.java)
            intent.putExtra("UID", mCard!!.uid)
            intent.putExtra("Card", mCard!!.asBundle)
            startActivityForResult(intent, REQUEST_CODE_SEND_PAYMENT)
        }
    }

    override fun onResume() {
        super.onResume()
        nfcManager!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        nfcManager!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        nfcManager!!.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var data = data
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_VERIFY_CARD ->
                // action when erase wallet
                if (resultCode == Activity.RESULT_OK) {
                    if (activity != null)
                        activity!!.finish()
                }

            REQUEST_CODE_ENTER_NEW_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.extras != null && data.extras!!.containsKey("confirmPIN")) {
                        val intent = Intent(context, RequestPINActivity::class.java)
                        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
                        intent.putExtra("UID", mCard!!.uid)
                        intent.putExtra("Card", mCard!!.asBundle)
                        newPIN = data.getStringExtra("newPIN")
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(context, RequestPINActivity::class.java)
                        intent.putExtra("newPIN", data.getStringExtra("newPIN"))
                        intent.putExtra("mode", RequestPINActivity.Mode.ConfirmNewPIN.toString())
                        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN)
                    }
                }
            }
            REQUEST_CODE_ENTER_NEW_PIN2 -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.extras != null && data.extras!!.containsKey("confirmPIN2")) {
                        val intent = Intent(context, RequestPINActivity::class.java)
                        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
                        intent.putExtra("UID", mCard!!.uid)
                        intent.putExtra("Card", mCard!!.asBundle)
                        newPIN2 = data.getStringExtra("newPIN2")
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(context, RequestPINActivity::class.java)
                        intent.putExtra("newPIN2", data.getStringExtra("newPIN2"))
                        intent.putExtra("mode", RequestPINActivity.Mode.ConfirmNewPIN2.toString())
                        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN2)
                    }
                }
            }
            REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (newPIN == "")
                    newPIN = mCard!!.pin

                if (newPIN2 == "")
                    newPIN2 = PINStorage.getPIN2()

                val pinSwapWarningDialog = PINSwapWarningDialog()
                pinSwapWarningDialog.setOnRefreshPage { this.startSwapPINActivity() }
                val bundle = Bundle()
                if (!PINStorage.isDefaultPIN(newPIN) || !PINStorage.isDefaultPIN2(newPIN2))
                    bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_forget))
                else
                    bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_use_default))
                pinSwapWarningDialog.arguments = bundle
                pinSwapWarningDialog.show(activity!!.fragmentManager, PINSwapWarningDialog.TAG)
            }

            REQUEST_CODE_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    data = Intent()
                    data.putExtra("UID", mCard!!.uid)
                    data.putExtra("Card", mCard!!.asBundle)
                    data.putExtra("modification", "delete")
                } else
                    data.putExtra("modification", "update")

                if (activity != null) {
                    activity!!.setResult(Activity.RESULT_OK, data)
                    activity!!.finish()
                }
            } else {
                if (data != null && data.extras != null && data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                    val updatedCard = TangemCard(data.getStringExtra("UID"))
                    updatedCard.LoadFromBundle(data.getBundleExtra("Card"))
                    mCard = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(context, RequestPINActivity::class.java)
                    intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
                    intent.putExtra("UID", mCard!!.uid)
                    intent.putExtra("Card", mCard!!.asBundle)
                    startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey("message")) {
                        mCard!!.error = data.getStringExtra("message")
                    }
                }
            }
            REQUEST_CODE_REQUEST_PIN2_FOR_PURGE -> if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(context, PurgeActivity::class.java)
                intent.putExtra("UID", mCard!!.uid)
                intent.putExtra("Card", mCard!!.asBundle)
                startActivityForResult(intent, REQUEST_CODE_PURGE)
            }
            REQUEST_CODE_PURGE -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    data = Intent()
                    data.putExtra("UID", mCard!!.uid)
                    data.putExtra("Card", mCard!!.asBundle)
                    data.putExtra("modification", "delete")
                } else {
                    data.putExtra("modification", "update")
                }
                if (activity != null) {
                    activity!!.setResult(Activity.RESULT_OK, data)
                    activity!!.finish()
                }
            } else {
                if (data != null && data.extras != null && data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                    val updatedCard = TangemCard(data.getStringExtra("UID"))
                    updatedCard.LoadFromBundle(data.getBundleExtra("Card"))
                    mCard = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(context, RequestPINActivity::class.java)
                    intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
                    intent.putExtra("UID", mCard!!.uid)
                    intent.putExtra("Card", mCard!!.asBundle)
                    startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_PURGE)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey("message")) {
                        mCard!!.error = data.getStringExtra("message")
                    }
                }
                updateViews()
            }
            REQUEST_CODE_SEND_PAYMENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    srlLoadedWallet!!.postDelayed({ this.refresh() }, 10000)
                    srlLoadedWallet!!.isRefreshing = true
                    mCard!!.clearInfo()
                    updateViews()
                }

                if (data != null && data.extras != null) {
                    if (data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                        val updatedCard = TangemCard(data.getStringExtra("UID"))
                        updatedCard.LoadFromBundle(data.getBundleExtra("Card"))
                        mCard = updatedCard
                    }
                    if (data.extras!!.containsKey("message")) {
                        if (resultCode == Activity.RESULT_OK) {
                            mCard!!.message = data.getStringExtra("message")
                        } else {
                            mCard!!.error = data.getStringExtra("message")
                        }
                    }
                    updateViews()
                }
            }
        }
    }

    fun refresh() {
        srlLoadedWallet!!.isRefreshing = true
        mCard!!.clearInfo()
        mCard!!.error = null
        mCard!!.message = null

        val needResendTX = LastSignStorage.getNeedTxSend(mCard!!.wallet)

        updateViews()

        val engine = CoinEngineFactory.Create(mCard!!.blockchain)

        if ((mCard!!.isOnlineVerified == null || !mCard!!.isOnlineVerified) && onlineVerifyTask == null) {
            onlineVerifyTask = OnlineVerifyTask()
            onlineVerifyTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, VerificationServerProtocol.Verify.prepare(mCard))
        }

        if (mCard!!.blockchain == Blockchain.Bitcoin || mCard!!.blockchain == Blockchain.BitcoinTestNet) {
            val data = SharedData(SharedData.COUNT_REQUEST)
            mCard!!.resetFailedBalanceRequestCounter()
            mCard!!.setIsBalanceEqual(true)
            for (i in 0 until data.allRequest) {
                val nodeAddress = engine!!.GetNextNode(mCard)
                val nodePort = engine.GetNextNodePort(mCard)
                val connectTaskEx = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort, data)
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(mCard!!.wallet))

                val updateWalletInfoTask = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort, data)
                updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(mCard!!.wallet))
            }

            val taskRate = RateInfoTask(this@LoadedWallet)
            val rate = ExchangeRequest.GetRate(mCard!!.wallet, "bitcoin", "bitcoin")
            taskRate.execute(rate)


        } else if (mCard!!.blockchain == Blockchain.BitcoinCash || mCard!!.blockchain == Blockchain.BitcoinCashTestNet) {
            mCard!!.resetFailedBalanceRequestCounter()
            val data = SharedData(SharedData.COUNT_REQUEST)
            mCard!!.setIsBalanceEqual(true)
            for (i in 0 until data.allRequest) {
                val nodeAddress = engine!!.GetNextNode(mCard)
                val nodePort = engine.GetNextNodePort(mCard)
                val connectTaskEx = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort, data)
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(mCard!!.wallet))
            }

            val nodeAddress = engine!!.GetNode(mCard)
            val nodePort = engine.GetNodePort(mCard)

            val updateWalletInfoTask = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort, data)
            updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(mCard!!.wallet))

            val taskRate = RateInfoTask(this@LoadedWallet)
            val rate = ExchangeRequest.GetRate(mCard!!.wallet, "bitcoin-cash", "bitcoin-cash")
            taskRate.execute(rate)


        } else if (mCard!!.blockchain == Blockchain.Ethereum || mCard!!.blockchain == Blockchain.EthereumTestNet) {
            val updateETH = ETHRequestTask(this@LoadedWallet, mCard!!.blockchain)
            val reqETH = InfuraRequest.GetBalance(mCard!!.wallet)
            reqETH.id = 67
            reqETH.setBlockchain(mCard!!.blockchain)

            val reqNonce = InfuraRequest.GetOutTransactionCount(mCard!!.wallet)
            reqNonce.id = 67
            reqNonce.setBlockchain(mCard!!.blockchain)

            updateETH.execute(reqETH, reqNonce)

            val taskRate = RateInfoTask(this@LoadedWallet)
            val rate = ExchangeRequest.GetRate(mCard!!.wallet, "ethereum", "ethereum")
            taskRate.execute(rate)

        } else if (mCard!!.blockchain == Blockchain.Token) {
            val updateETH = ETHRequestTask(this@LoadedWallet, mCard!!.blockchain)
            val reqETH = InfuraRequest.GetTokenBalance(mCard!!.wallet, engine!!.GetContractAddress(mCard), engine.GetTokenDecimals(mCard))
            reqETH.id = 67
            reqETH.setBlockchain(mCard!!.blockchain)

            val reqBalance = InfuraRequest.GetBalance(mCard!!.wallet)
            reqBalance.id = 67
            reqBalance.setBlockchain(mCard!!.blockchain)

            val reqNonce = InfuraRequest.GetOutTransactionCount(mCard!!.wallet)
            reqNonce.id = 67
            reqNonce.setBlockchain(mCard!!.blockchain)
            updateETH.execute(reqETH, reqNonce, reqBalance)


            val taskRate = RateInfoTask(this@LoadedWallet)
            val rate = ExchangeRequest.GetRate(mCard!!.wallet, "ethereum", "ethereum")
            taskRate.execute(rate)
        }

        if (needResendTX)
            sendTransaction(LastSignStorage.getTxForSend(mCard!!.wallet))


//        if (activity!!.intent.extras!!.containsKey(NfcAdapter.EXTRA_TAG)) {
//            val tag = activity!!.intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
//            if (tag != null) {
//                onTagDiscovered(tag)
//            }
//        }
    }

    override fun onTagDiscovered(tag: Tag) {
        startVerify(tag)
    }

    fun prepareResultIntent(): Intent {
        val data = Intent()
        data.putExtra("UID", mCard!!.uid)
        data.putExtra("Card", mCard!!.asBundle)
        return data
    }

    override fun OnReadStart(cardProtocol: CardProtocol) {
        rlProgressBar.post { rlProgressBar.visibility = View.VISIBLE }
    }

    override fun OnReadFinish(cardProtocol: CardProtocol?) {
        verifyCardTask = null

        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar.post {
                    rlProgressBar.visibility = View.GONE
                    mCardProtocol = cardProtocol
                    refresh()
                }
            } else {
                // remove last UIDs because of error and no card read
                rlProgressBar.post {
                    lastReadSuccess = false
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported)
                        if (!NoExtendedLengthSupportDialog.allreadyShowed)
                            NoExtendedLengthSupportDialog().show(activity!!.fragmentManager, NoExtendedLengthSupportDialog.TAG)
                        else
                            Toast.makeText(context, R.string.try_to_scan_again, Toast.LENGTH_SHORT).show()
                }
            }
        }

        rlProgressBar.postDelayed({
            try {
                rlProgressBar.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun OnReadProgress(protocol: CardProtocol, progress: Int) {

    }

    override fun OnReadCancel() {
        verifyCardTask = null
        rlProgressBar.postDelayed({
            try {
                rlProgressBar.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun OnReadWait(msec: Int) {
        WaitSecurityDelayDialog.OnReadWait(activity!!, msec)
    }

    override fun OnReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(activity!!, timeout)
    }

    override fun OnReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(activity!!)
    }

    fun updateViews() {
        try {
            if (timerHideErrorAndMessage != null) {
                timerHideErrorAndMessage!!.cancel()
                timerHideErrorAndMessage = null
            }
            tvCardID.text = mCard!!.cidDescription

            if (mCard!!.error == null || mCard!!.error.isEmpty()) {
                tvError.visibility = View.GONE
                tvError.text = ""
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = mCard!!.error
            }

            val needResendTX = LastSignStorage.getNeedTxSend(mCard!!.wallet)

            if ((mCard!!.message == null || mCard!!.message.isEmpty()) && !needResendTX) {
                tvMessage!!.text = ""
                tvMessage!!.visibility = View.GONE
            } else {
                if (needResendTX) {

                } else {
                    tvMessage!!.text = mCard!!.message
                }

                tvMessage!!.visibility = View.VISIBLE
            }

            val engine = CoinEngineFactory.Create(mCard!!.blockchain)

            if (mCard!!.blockchain == Blockchain.Bitcoin || mCard!!.blockchain == Blockchain.BitcoinTestNet) {

                val validator = BalanceValidator()
                validator.Check(mCard)
                tvBalanceLine1.text = validator.GetFirstLine()
                tvBalanceLine2.text = validator.GetSecondLine()
                tvBalanceLine1.setTextColor(ContextCompat.getColor(context!!, validator.GetColor()))
            }

            if (engine!!.HasBalanceInfo(mCard) || mCard!!.offlineBalance == null) {
                if (mCard!!.blockchain == Blockchain.Token) {
                    val html = Html.fromHtml(engine.GetBalanceWithAlter(mCard))
                    tvBalance.text = html
                } else {
                    tvBalance.text = engine.GetBalanceWithAlter(mCard)
                }

                tvBalanceEquivalent.text = engine.GetBalanceEquivalent(mCard)
                tvOffline.visibility = View.INVISIBLE
            } else {
                val offlineAmount = engine.ConvertByteArrayToAmount(mCard, mCard!!.offlineBalance)
                if (mCard!!.blockchain == Blockchain.Token) {
                    tvBalance.setText(R.string.not_implemented)
                } else {
                    tvBalance.text = engine.GetAmountDescription(mCard, offlineAmount)
                }

                tvBalanceEquivalent.text = engine.GetAmountEqualentDescriptor(mCard, offlineAmount)
                tvOffline.visibility = View.VISIBLE
            }

            if (!mCard!!.amountEquivalentDescriptionAvailable) {

            } else {
                tvBalanceEquivalent.error = null
            }

            tvWallet.text = mCard!!.wallet

            tvInputs.text = mCard!!.inputsDescription
            when {
                mCard!!.lastInputDescription.contains("awaiting") -> tvInputs.setTextColor(ContextCompat.getColor(context!!, R.color.not_confirmed))
                mCard!!.lastInputDescription.contains("None") -> tvInputs.setTextColor(ContextCompat.getColor(context!!, R.color.primary_dark))
                else -> tvInputs.setTextColor(ContextCompat.getColor(context!!, R.color.confirmed))
            }

            tvBlockchain.text = mCard!!.blockchainName

            tvValidationNode.text = mCard!!.validationNodeDescription

            if (mCard!!.useDefaultPIN1()!!) {
                ivPIN.setImageResource(R.drawable.unlock_pin1)
                ivPIN.setOnClickListener { Toast.makeText(context, R.string.this_banknote_protected_default_PIN1_code, Toast.LENGTH_LONG).show() }
            } else {
                ivPIN!!.setImageResource(R.drawable.lock_pin1)
                ivPIN!!.setOnClickListener { Toast.makeText(context, R.string.this_banknote_protected_user_PIN1_code, Toast.LENGTH_LONG).show() }
            }

            if (mCard!!.pauseBeforePIN2 > 0 && (mCard!!.useDefaultPIN2()!! || !mCard!!.useSmartSecurityDelay())) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.timer)
                ivPIN2orSecurityDelay.setOnClickListener { Toast.makeText(context, String.format("This banknote will enforce %.0f seconds security delay for all operations requiring PIN2 code", mCard!!.pauseBeforePIN2 / 1000.0), Toast.LENGTH_LONG).show() }

            } else if (mCard!!.useDefaultPIN2()!!) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2)
                ivPIN2orSecurityDelay.setOnClickListener { Toast.makeText(context, R.string.this_banknote_protected_default_PIN2_code, Toast.LENGTH_LONG).show() }
            } else {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2)
                ivPIN2orSecurityDelay.setOnClickListener { Toast.makeText(context, R.string.this_banknote_protected_user_PIN2_code, Toast.LENGTH_LONG).show() }
            }

            if (mCard!!.useDevelopersFirmware()!!) {
                ivDeveloperVersion.setImageResource(R.drawable.ic_developer_version)
                ivDeveloperVersion.visibility = View.VISIBLE
                ivDeveloperVersion.setOnClickListener { v -> Toast.makeText(context, R.string.unlocked_banknote_only_development_use, Toast.LENGTH_LONG).show() }
            } else
                ivDeveloperVersion.visibility = View.INVISIBLE


            btnExtract!!.isEnabled = mCard!!.hasBalanceInfo()

            tvIssuer.text = mCard!!.issuerDescription

            mCard!!.error = null
            mCard!!.message = null

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun openVerifyCard(cardProtocol: CardProtocol) {
        val intent = Intent(context, VerifyCardActivity::class.java)
        intent.putExtra("UID", cardProtocol.card.uid)
        intent.putExtra("Card", cardProtocol.card.asBundle)
        startActivityForResult(intent, REQUEST_CODE_VERIFY_CARD)
    }

    private fun startVerify(tag: Tag?) {
        try {
            val isoDep = IsoDep.get(tag)
                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))
            val uid = tag!!.id
            val sUID = Util.byteArrayToHexString(uid)
            if (mCard!!.uid != sUID) {
//                Log.d(TAG, "Invalid UID: $sUID")
                nfcManager!!.ignoreTag(isoDep.tag)
                return
            } else {
//                Log.v(TAG, "UID: $sUID")
            }

            if (lastReadSuccess) {
                isoDep.timeout = 1000
            } else {
                isoDep.timeout = 65000
            }

            verifyCardTask = VerifyCardTask(context, mCard, nfcManager, isoDep, this)
            verifyCardTask!!.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doShareWallet(useURI: Boolean) {
        if (useURI) {
            val txtShare = CoinEngineFactory.Create(mCard!!.blockchain)!!.getShareWalletURI(mCard).toString()
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, "Wallet address")
            intent.putExtra(Intent.EXTRA_TEXT, txtShare)

            val packageManager = activity!!.packageManager
            val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            val isIntentSafe = activities.size > 0

            if (isIntentSafe) {
                // create intent to show chooser
                val chooser = Intent.createChooser(intent, getString(R.string.share_wallet_address_with))

                // verify the intent will resolve to at least one activity
                if (intent.resolveActivity(activity!!.packageManager) != null) {
                    startActivity(chooser)
                }
            } else {
                val clipboard = activity!!.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip = ClipData.newPlainText(txtShare, txtShare)
                Toast.makeText(context, R.string.copied_clipboard, Toast.LENGTH_LONG).show()
            }
        } else {
            val txtShare = mCard!!.wallet
            val clipboard = activity!!.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = ClipData.newPlainText(txtShare, txtShare)
            Toast.makeText(context, R.string.copied_clipboard, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendTransaction(tx: String) {
        val engine = CoinEngineFactory.Create(mCard!!.blockchain)
        if (mCard!!.blockchain == Blockchain.Ethereum || mCard!!.blockchain == Blockchain.EthereumTestNet || mCard!!.blockchain == Blockchain.Token) {
            val task = ETHRequestTask(this@LoadedWallet, mCard!!.blockchain)
            val req = InfuraRequest.SendTransaction(mCard!!.wallet, tx)
            req.id = 67
            req.setBlockchain(mCard!!.blockchain)
            task.execute(req)
        } else if (mCard!!.blockchain == Blockchain.Bitcoin || mCard!!.blockchain == Blockchain.BitcoinTestNet) {
            val nodeAddress = engine!!.GetNode(mCard)
            val nodePort = engine.GetNodePort(mCard)

            val connectTask = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort)
            connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.Broadcast(mCard!!.wallet, tx))
        } else if (mCard!!.blockchain == Blockchain.BitcoinCash || mCard!!.blockchain == Blockchain.BitcoinCashTestNet) {
            val nodeAddress = engine!!.GetNode(mCard)
            val nodePort = engine.GetNodePort(mCard)

            val connectTask = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort)
            connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.Broadcast(mCard!!.wallet, tx))
        }
    }

    private fun startSwapPINActivity() {
        val intent = Intent(context, SwapPINActivity::class.java)
        intent.putExtra("UID", mCard!!.uid)
        intent.putExtra("Card", mCard!!.asBundle)
        intent.putExtra("newPIN", newPIN)
        intent.putExtra("newPIN2", newPIN2)
        startActivityForResult(intent, REQUEST_CODE_SWAP_PIN)
    }

}