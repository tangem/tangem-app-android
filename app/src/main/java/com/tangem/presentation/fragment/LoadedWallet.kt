package com.tangem.presentation.fragment

import android.app.Activity
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.android.volley.BuildConfig
import com.google.zxing.WriterException
import com.tangem.data.network.Server
import com.tangem.data.network.VolleyHelper
import com.tangem.data.network.model.ResponseVerify
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.util.*

class LoadedWallet : Fragment(), NfcAdapter.ReaderCallback, CardProtocol.Notifications, VolleyHelper.IRequestCardVerify, SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (BuildConfig.DEBUG && this@LoadedWallet.isAdded) {
            debugNewRequestVerify = sharedPreferences!!.getBoolean(getString(R.string.key_debug_new_request_verify), false)
            debugNewRequestVerifyShowJson = sharedPreferences.getBoolean(getString(R.string.key_debug_new_request_verify_show_json), false)
        }
    }

    override fun success(responseVerify: ResponseVerify) {
//        Toast.makeText(activity, responseVerify.results!![1].CID, Toast.LENGTH_SHORT).show()
    }

    override fun error(error: String) {

    }

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

        private const val URL_CARD_VERIFY = Server.API.Method.VERIFY
    }

    private var singleToast: Toast? = null
    private var nfcManager: NfcManager? = null
    private var volleyHelper: VolleyHelper? = null
    var card: TangemCard? = null
    private var lastTag: Tag? = null

    var srlLoadedWallet: SwipeRefreshLayout? = null

    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0
    private var timerHideErrorAndMessage: Timer? = null
    private var newPIN = ""
    private var newPIN2 = ""
    private var cardProtocol: CardProtocol? = null

    private var onlineVerifyTask: OnlineVerifyTask? = null

    private var sp: SharedPreferences? = null
    private var debugNewRequestVerify: Boolean = false
    private var debugNewRequestVerifyShowJson: Boolean = false

    private inner class OnlineVerifyTask : VerificationServerTask() {

        override fun onPostExecute(requests: List<VerificationServerProtocol.Request>) {
            super.onPostExecute(requests)
            onlineVerifyTask = null

            for (request in requests) {
                if (request.error == null) {
                    val answer = request.answer as VerificationServerProtocol.Verify.Answer
                    if (answer.error == null) {
                        card!!.isOnlineVerified = answer.results[0].passed
//                        Log.i(TAG, "isOnlineVerified = " + answer.results[0].passed)
                    } else {
                        card!!.isOnlineVerified = null
//                        Log.i(TAG, "isOnlineVerified = null")
                    }
                } else {
                    card!!.isOnlineVerified = null
                }
            }
            srlLoadedWallet!!.isRefreshing = false
        }
    }

    private fun requestVerify() {
        if ((card!!.isOnlineVerified == null || !card!!.isOnlineVerified) && onlineVerifyTask == null) {
            if (debugNewRequestVerify) {
                volleyHelper!!.requestCardVerify(card!!)

                if (debugNewRequestVerifyShowJson)
                    this.activity?.let { volleyHelper!!.requestCardVerifyShowResponse(it, card!!) }

            } else {
                onlineVerifyTask = OnlineVerifyTask()
                onlineVerifyTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, VerificationServerProtocol.Verify.prepare(card))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sp = PreferenceManager.getDefaultSharedPreferences(activity)
        nfcManager = NfcManager(activity, this)

        card = TangemCard(activity!!.intent.getStringExtra(TangemCard.EXTRA_CARD))
        card!!.loadFromBundle(activity!!.intent.extras.getBundle(TangemCard.EXTRA_CARD))

        lastTag = activity!!.intent.getParcelableExtra(MainActivity.EXTRA_LAST_DISCOVERED_TAG)

        volleyHelper = VolleyHelper(this)

        debugNewRequestVerify = sp!!.getBoolean(getString(R.string.key_debug_new_request_verify), false)
        debugNewRequestVerifyShowJson = sp!!.getBoolean(getString(R.string.key_debug_new_request_verify_show_json), false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_loaded_wallet, container, false)
        srlLoadedWallet = v.findViewById(R.id.srlLoadedWallet)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (card!!.blockchain == Blockchain.Token)
            tvBalance.setSingleLine(false)

        ivTangemCard.setImageResource(card!!.cardImageResource)

        val engine = CoinEngineFactory.create(card!!.blockchain)
        val visibleFlag = engine?.inOutPutVisible() ?: true

        try {
            ivQR.setImageBitmap(UtilHelper.generateQrCode(engine.getShareWalletUri(card).toString()))
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        updateViews()

        if (!card!!.hasBalanceInfo()) {
            srlLoadedWallet!!.isRefreshing = true
            srlLoadedWallet!!.postDelayed({ this.refresh() }, 1000)
        }

        requestVerify()

        startVerify(lastTag)

        tvWallet.text = card!!.wallet

        // set listeners
        srlLoadedWallet!!.setOnRefreshListener { this.refresh() }
        btnLookup.setOnClickListener {
            val engineClick = CoinEngineFactory.create(card!!.blockchain)
            val browserIntent = Intent(Intent.ACTION_VIEW, engineClick.getShareWalletUriExplorer(card))
            startActivity(browserIntent)
        }
        btnCopy.setOnClickListener { doShareWallet(false) }
        tvWallet.setOnClickListener { doShareWallet(false) }
        ivQR.setOnClickListener { doShareWallet(true) }
        btnLoad.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, CoinEngineFactory.create(card!!.blockchain)!!.getShareWalletUri(card))
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                showSingleToast(R.string.no_compatible_wallet)
            }
        }
        btnDetails.setOnClickListener {
            if (cardProtocol != null)
                openVerifyCard(cardProtocol!!)
            else
                showSingleToast(R.string.need_attach_card_again)
        }
        btnScanAgain.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }
        btnExtract.setOnClickListener {
            if (!card!!.hasBalanceInfo()) {
                showSingleToast(R.string.cannot_obtain_data_from_blockchain)
            } else if (!engine.isBalanceNotZero(card))
                showSingleToast(R.string.wallet_empty)
            else if (!engine.isBalanceAlterNotZero(card))
                showSingleToast(R.string.not_enough_funds)
            else if (engine.awaitingConfirmation(card))
                showSingleToast(R.string.please_wait_while_previous)
            else if (!engine.checkUnspentTransaction(card))
                showSingleToast(R.string.please_wait_for_confirmation)
            else if (card!!.remainingSignatures == 0)
                showSingleToast(R.string.card_has_no_remaining_signature)
            else {
                val intent = Intent(context, PreparePaymentActivity::class.java)
                intent.putExtra("UID", card!!.uid)
                intent.putExtra("Card", card!!.asBundle)
                startActivityForResult(intent, REQUEST_CODE_SEND_PAYMENT)
            }
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
                        val intent = Intent(context, PinRequestActivity::class.java)
                        intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                        intent.putExtra("UID", card!!.uid)
                        intent.putExtra("Card", card!!.asBundle)
                        newPIN = data.getStringExtra("newPIN")
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(context, PinRequestActivity::class.java)
                        intent.putExtra("newPIN", data.getStringExtra("newPIN"))
                        intent.putExtra("mode", PinRequestActivity.Mode.ConfirmNewPIN.toString())
                        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN)
                    }
                }
            }
            REQUEST_CODE_ENTER_NEW_PIN2 -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.extras != null && data.extras!!.containsKey("confirmPIN2")) {
                        val intent = Intent(context, PinRequestActivity::class.java)
                        intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                        intent.putExtra("UID", card!!.uid)
                        intent.putExtra("Card", card!!.asBundle)
                        newPIN2 = data.getStringExtra("newPIN2")
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(context, PinRequestActivity::class.java)
                        intent.putExtra("newPIN2", data.getStringExtra("newPIN2"))
                        intent.putExtra("mode", PinRequestActivity.Mode.ConfirmNewPIN2.toString())
                        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN2)
                    }
                }
            }
            REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (newPIN == "")
                    newPIN = card!!.pin

                if (newPIN2 == "")
                    newPIN2 = PINStorage.getPIN2()

                val pinSwapWarningDialog = PINSwapWarningDialog()
                pinSwapWarningDialog.setOnRefreshPage { startSwapPINActivity() }
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
                    data.putExtra("UID", card!!.uid)
                    data.putExtra("Card", card!!.asBundle)
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
                    updatedCard.loadFromBundle(data.getBundleExtra("Card"))
                    card = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(context, PinRequestActivity::class.java)
                    intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                    intent.putExtra("UID", card!!.uid)
                    intent.putExtra("Card", card!!.asBundle)
                    startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey("message")) {
                        card!!.error = data.getStringExtra("message")
                    }
                }
            }
            REQUEST_CODE_REQUEST_PIN2_FOR_PURGE -> if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(context, PurgeActivity::class.java)
                intent.putExtra("UID", card!!.uid)
                intent.putExtra("Card", card!!.asBundle)
                startActivityForResult(intent, REQUEST_CODE_PURGE)
            }
            REQUEST_CODE_PURGE -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    data = Intent()
                    data.putExtra("UID", card!!.uid)
                    data.putExtra("Card", card!!.asBundle)
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
                    updatedCard.loadFromBundle(data.getBundleExtra("Card"))
                    card = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(context, PinRequestActivity::class.java)
                    intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                    intent.putExtra("UID", card!!.uid)
                    intent.putExtra("Card", card!!.asBundle)
                    startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_PURGE)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey("message")) {
                        card!!.error = data.getStringExtra("message")
                    }
                }
                updateViews()
            }
            REQUEST_CODE_SEND_PAYMENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    srlLoadedWallet!!.postDelayed({ this.refresh() }, 10000)
                    srlLoadedWallet!!.isRefreshing = true
                    card!!.clearInfo()
                    updateViews()
                }

                if (data != null && data.extras != null) {
                    if (data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                        val updatedCard = TangemCard(data.getStringExtra("UID"))
                        updatedCard.loadFromBundle(data.getBundleExtra("Card"))
                        card = updatedCard
                    }
                    if (data.extras!!.containsKey("message")) {
                        if (resultCode == Activity.RESULT_OK) {
                            card!!.message = data.getStringExtra("message")
                        } else {
                            card!!.error = data.getStringExtra("message")
                        }
                    }
                    updateViews()
                }
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        startVerify(tag)
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar.post { rlProgressBar.visibility = View.VISIBLE }
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {

    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        verifyCardTask = null

        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar.post {
                    rlProgressBar.visibility = View.GONE
                    this.cardProtocol = cardProtocol
                    refresh()
                }
            } else {
                // remove last UIDs because of error and no card read
                rlProgressBar.post {
                    lastReadSuccess = false
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported)
                        if (!NoExtendedLengthSupportDialog.allReadyShowed)
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

    override fun onReadCancel() {
        verifyCardTask = null
        rlProgressBar.postDelayed({
            try {
                rlProgressBar.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.OnReadWait(activity!!, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(activity!!, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(activity!!)
    }

    fun updateViews() {
        try {
            if (timerHideErrorAndMessage != null) {
                timerHideErrorAndMessage!!.cancel()
                timerHideErrorAndMessage = null
            }

            if (card!!.error == null || card!!.error.isEmpty()) {
                tvError.visibility = View.GONE
                tvError.text = ""
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = card!!.error
            }

            val needResendTX = LastSignStorage.getNeedTxSend(card!!.wallet)

            if ((card!!.message == null || card!!.message.isEmpty()) && !needResendTX) {
                tvMessage!!.text = ""
                tvMessage!!.visibility = View.GONE
            } else {
                if (needResendTX) {

                } else {
                    tvMessage!!.text = card!!.message
                }

                tvMessage!!.visibility = View.VISIBLE
            }

            val engine = CoinEngineFactory.create(card!!.blockchain)

            if (card!!.blockchain == Blockchain.Bitcoin || card!!.blockchain == Blockchain.BitcoinTestNet) {

                val validator = BalanceValidator()
                validator.Check(card)
                tvBalanceLine1.text = validator.firstLine
                tvBalanceLine2.text = validator.secondLine
                tvBalanceLine1.setTextColor(ContextCompat.getColor(context!!, validator.color))
            }

            if (engine!!.hasBalanceInfo(card) || card!!.offlineBalance == null) {
                if (card!!.blockchain == Blockchain.Token) {
                    val html = Html.fromHtml(engine.getBalanceWithAlter(card))
                    tvBalance.text = html
                } else
                    tvBalance.text = engine.getBalanceWithAlter(card)

            } else {
                val offlineAmount = engine.convertByteArrayToAmount(card, card!!.offlineBalance)
                if (card!!.blockchain == Blockchain.Token) {
                    tvBalance.setText(R.string.not_implemented)
                } else
                    tvBalance.text = engine.getAmountDescription(card, offlineAmount)
            }

            tvWallet.text = card!!.wallet

            tvBlockchain.text = card!!.blockchainName

            btnExtract!!.isEnabled = card!!.hasBalanceInfo()

            card!!.error = null
            card!!.message = null

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refresh() {
        srlLoadedWallet!!.isRefreshing = true
        card!!.clearInfo()
        card!!.error = null
        card!!.message = null

        val needResendTX = LastSignStorage.getNeedTxSend(card!!.wallet)

        updateViews()

        val engine = CoinEngineFactory.create(card!!.blockchain)

        requestVerify()

        if (card!!.blockchain == Blockchain.Bitcoin || card!!.blockchain == Blockchain.BitcoinTestNet) {
            val data = SharedData(SharedData.COUNT_REQUEST)
            card!!.resetFailedBalanceRequestCounter()
            card!!.setIsBalanceEqual(true)

            for (i in 0 until data.allRequest) {
                val nodeAddress = engine!!.getNextNode(card)
                val nodePort = engine.getNextNodePort(card)

                // check balance
                val connectTaskEx = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort, data)
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card!!.wallet))




                Log.i("effefefe23", nodeAddress)
                Log.i("effefefe23", nodePort.toString())

                val client = OkHttpClient.Builder().build()
                val request = Request.Builder()
                        .url("ws://btc.cihar.com:50001")
                        .build()
                val wsc = WebSocketClass()
                val ws = client.newWebSocket(request, wsc)



                // list unspent input
                val updateWalletInfoTask = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort, data)
                updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(card!!.wallet))
            }

            // course request
            val taskRate = RateInfoTask(this@LoadedWallet)
            val rate = ExchangeRequest.GetRate(card!!.wallet, "bitcoin", "bitcoin")
            taskRate.execute(rate)

        } else if (card!!.blockchain == Blockchain.BitcoinCash || card!!.blockchain == Blockchain.BitcoinCashTestNet) {
            card!!.resetFailedBalanceRequestCounter()
            val data = SharedData(SharedData.COUNT_REQUEST)
            card!!.setIsBalanceEqual(true)
            for (i in 0 until data.allRequest) {
                val nodeAddress = engine!!.getNextNode(card)
                val nodePort = engine.getNextNodePort(card)

                // check balance
                val connectTaskEx = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort, data)
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card!!.wallet))
            }

            val nodeAddress = engine!!.getNode(card)
            val nodePort = engine.getNodePort(card)

            // list unspent input
            val updateWalletInfoTask = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort, data)
            updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(card!!.wallet))

            // course request
            val taskRate = RateInfoTask(this@LoadedWallet)
            val rate = ExchangeRequest.GetRate(card!!.wallet, "bitcoin-cash", "bitcoin-cash")
            taskRate.execute(rate)

        } else if (card!!.blockchain == Blockchain.Ethereum || card!!.blockchain == Blockchain.EthereumTestNet) {
            val updateETH = ETHRequestTask(this@LoadedWallet, card!!.blockchain)
            val reqETH = InfuraRequest.GetBalance(card!!.wallet)
            reqETH.id = 67
            reqETH.setBlockchain(card!!.blockchain)

            val reqNonce = InfuraRequest.GetOutTransactionCount(card!!.wallet)
            reqNonce.id = 67
            reqNonce.setBlockchain(card!!.blockchain)

            updateETH.execute(reqETH, reqNonce)

            // course request
            val taskRate = RateInfoTask(this@LoadedWallet)
            val rate = ExchangeRequest.GetRate(card!!.wallet, "ethereum", "ethereum")
            taskRate.execute(rate)

        } else if (card!!.blockchain == Blockchain.Token) {
            val updateETH = ETHRequestTask(this@LoadedWallet, card!!.blockchain)
            val reqETH = InfuraRequest.GetTokenBalance(card!!.wallet, engine!!.getContractAddress(card), engine.getTokenDecimals(card))
            reqETH.id = 67
            reqETH.setBlockchain(card!!.blockchain)

            val reqBalance = InfuraRequest.GetBalance(card!!.wallet)
            reqBalance.id = 67
            reqBalance.setBlockchain(card!!.blockchain)

            val reqNonce = InfuraRequest.GetOutTransactionCount(card!!.wallet)
            reqNonce.id = 67
            reqNonce.setBlockchain(card!!.blockchain)
            updateETH.execute(reqETH, reqNonce, reqBalance)

            // course request
            val taskRate = RateInfoTask(this@LoadedWallet)
            val rate = ExchangeRequest.GetRate(card!!.wallet, "ethereum", "ethereum")
            taskRate.execute(rate)
        }

        if (needResendTX)
            sendTransaction(LastSignStorage.getTxForSend(card!!.wallet))
    }

    fun prepareResultIntent(): Intent {
        val data = Intent()
        data.putExtra("UID", card!!.uid)
        data.putExtra("Card", card!!.asBundle)
        return data
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
            if (card!!.uid != sUID) {
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

            verifyCardTask = VerifyCardTask(context, card, nfcManager, isoDep, this)
            verifyCardTask!!.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doShareWallet(useURI: Boolean) {
        if (useURI) {
            val txtShare = CoinEngineFactory.create(card!!.blockchain)!!.getShareWalletUri(card).toString()
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
            val txtShare = card!!.wallet
            val clipboard = activity!!.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = ClipData.newPlainText(txtShare, txtShare)
            Toast.makeText(context, R.string.copied_clipboard, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendTransaction(tx: String) {
        val engine = CoinEngineFactory.create(card!!.blockchain)
        if (card!!.blockchain == Blockchain.Ethereum || card!!.blockchain == Blockchain.EthereumTestNet || card!!.blockchain == Blockchain.Token) {
            val task = ETHRequestTask(this@LoadedWallet, card!!.blockchain)
            val req = InfuraRequest.SendTransaction(card!!.wallet, tx)
            req.id = 67
            req.setBlockchain(card!!.blockchain)
            task.execute(req)
        } else if (card!!.blockchain == Blockchain.Bitcoin || card!!.blockchain == Blockchain.BitcoinTestNet) {
            val nodeAddress = engine!!.getNode(card)
            val nodePort = engine.getNodePort(card)

            val connectTask = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort)
            connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.Broadcast(card!!.wallet, tx))
        } else if (card!!.blockchain == Blockchain.BitcoinCash || card!!.blockchain == Blockchain.BitcoinCashTestNet) {
            val nodeAddress = engine!!.getNode(card)
            val nodePort = engine.getNodePort(card)

            val connectTask = UpdateWalletInfoTask(this@LoadedWallet, nodeAddress, nodePort)
            connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.Broadcast(card!!.wallet, tx))
        }
    }

    private fun startSwapPINActivity() {
        val intent = Intent(context, PinSwapActivity::class.java)
        intent.putExtra("UID", card!!.uid)
        intent.putExtra("Card", card!!.asBundle)
        intent.putExtra("newPIN", newPIN)
        intent.putExtra("newPIN2", newPIN2)
        startActivityForResult(intent, REQUEST_CODE_SWAP_PIN)
    }

    private fun showSingleToast(text: Int) {
        if (singleToast == null || !singleToast!!.view.isShown) {
            if (singleToast != null)
                singleToast!!.cancel()
            singleToast = Toast.makeText(context, text, Toast.LENGTH_LONG)
            singleToast!!.show()
        }
    }

}