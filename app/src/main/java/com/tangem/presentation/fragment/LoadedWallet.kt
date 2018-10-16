package com.tangem.presentation.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.view.ContextThemeWrapper
import android.text.Html
import android.util.Log
import android.view.*
import android.widget.Toast
import com.google.zxing.WriterException
import com.tangem.data.network.ServerApiHelper
import com.tangem.data.network.ServerApiHelperElectrum
import com.tangem.data.network.model.InfuraResponse
import com.tangem.data.network.ElectrumRequest
import com.tangem.data.network.model.CardVerifyAndGetInfo
import com.tangem.data.nfc.VerifyCardTask
import com.tangem.domain.cardReader.CardProtocol
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.*
import com.tangem.presentation.activity.*
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.PINSwapWarningDialog
import com.tangem.presentation.dialog.ShowQRCodeDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.util.Util
import com.tangem.util.UtilHelper
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fr_loaded_wallet.*
import org.json.JSONException
import java.io.InputStream
import java.math.BigInteger
import java.util.*

class LoadedWallet : Fragment(), NfcAdapter.ReaderCallback, CardProtocol.Notifications, SharedPreferences.OnSharedPreferenceChangeListener {
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
        private const val REQUEST_CODE_RECEIVE_PAYMENT = 9
    }

    private var nfcManager: NfcManager? = null

    private var serverApiHelper: ServerApiHelper = ServerApiHelper()
    private var serverApiHelperElectrum: ServerApiHelperElectrum = ServerApiHelperElectrum()

    private var singleToast: Toast? = null
    private var card: TangemCard? = null
    private var lastTag: Tag? = null
    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0
    private var timerHideErrorAndMessage: Timer? = null
    private var newPIN = ""
    private var newPIN2 = ""
    private var cardProtocol: CardProtocol? = null
    private var sp: SharedPreferences? = null
    private val inactiveColor: ColorStateList by lazy { resources.getColorStateList(R.color.btn_dark) }
    private val activeColor: ColorStateList by lazy { resources.getColorStateList(R.color.colorAccent) }
    private var requestCounter = 0
    private var timerRepeatRefresh: Timer? = null
    private lateinit var localStorage: LocalStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcManager = NfcManager(activity, this)

        sp = PreferenceManager.getDefaultSharedPreferences(activity)

        card = TangemCard(activity!!.intent.getStringExtra(TangemCard.EXTRA_UID))
        card!!.loadFromBundle(activity!!.intent.extras.getBundle(TangemCard.EXTRA_CARD))

        lastTag = activity!!.intent.getParcelableExtra(MainActivity.EXTRA_LAST_DISCOVERED_TAG)

        localStorage = LocalStorage(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fr_loaded_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (card!!.blockchain == Blockchain.Token)
            tvBalance.setSingleLine(false)

        ivTangemCard.setImageBitmap(localStorage.getCardArtworkBitmap(card!!))

        val engine = CoinEngineFactory.create(card!!.blockchain)
        val visibleFlag = engine?.inOutPutVisible() ?: true

        try {
            ivQR.setImageBitmap(UtilHelper.generateQrCode(engine.getShareWalletUri(card).toString()))
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        btnExtract.isEnabled = false
        btnExtract.backgroundTintList = inactiveColor

        refresh()

        startVerify(lastTag)

        tvWallet.text = card!!.wallet

        // set listeners
        srl!!.setOnRefreshListener { refresh() }
        btnLookup.setOnClickListener {
            val engineClick = CoinEngineFactory.create(card!!.blockchain)
            val browserIntent = Intent(Intent.ACTION_VIEW, engineClick.getShareWalletUriExplorer(card))
            startActivity(browserIntent)
        }
        btnCopy.setOnClickListener { doShareWallet(false) }
        tvWallet.setOnClickListener { doShareWallet(false) }
        ivQR.setOnClickListener { doShareWallet(true) }
        btnLoad.setOnClickListener {
            //if (BuildConfig.DEBUG) {
            if (true) {
                val items = arrayOf<CharSequence>(getString(R.string.in_app), getString(R.string.load_via_share_address), getString(R.string.load_via_qr))//, getString(R.string.via_cryptonit), getString(R.string.via_kraken))
                val cw = android.view.ContextThemeWrapper(activity, R.style.AlertDialogTheme)
                val dialog = AlertDialog.Builder(cw).setItems(items
                ) { _, which ->
                    when (items[which]) {
                        getString(R.string.in_app) -> {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, CoinEngineFactory.create(card!!.blockchain)!!.getShareWalletUri(card))
                                intent.addCategory(Intent.CATEGORY_DEFAULT)
                                startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                showSingleToast(R.string.no_compatible_wallet)
                            }
                        }
                        getString(R.string.load_via_share_address) -> {
                            doShareWallet(true)
                        }
                        getString(R.string.load_via_qr) -> {
                            ShowQRCodeDialog.show(activity, engine.getShareWalletUri(card).toString())
                        }
//                        getString(R.string.via_cryptonit2) -> {
//                            val intent = Intent(context, PrepareCryptonitOtherAPIWithdrawalActivity::class.java)
//                            intent.putExtra("UID", card!!.uid)
//                            intent.putExtra("Card", card!!.asBundle)
//                            startActivityForResult(intent, REQUEST_CODE_RECEIVE_PAYMENT)
//                        }
                        getString(R.string.via_cryptonit) -> {
                            val intent = Intent(context, PrepareCryptonitWithdrawalActivity::class.java)
                            intent.putExtra("UID", card!!.uid)
                            intent.putExtra("Card", card!!.asBundle)
                            startActivityForResult(intent, REQUEST_CODE_RECEIVE_PAYMENT)
                        }
                        getString(R.string.via_kraken) -> {
                            val intent = Intent(context, PrepareKrakenWithdrawalActivity::class.java)
                            intent.putExtra("UID", card!!.uid)
                            intent.putExtra("Card", card!!.asBundle)
                            startActivityForResult(intent, REQUEST_CODE_RECEIVE_PAYMENT)
                        }
                        else -> {
                        }
                    }
                }
                val dlg = dialog.show()
                val wlp = dlg.window.attributes
                wlp.gravity = Gravity.BOTTOM
                dlg.window.attributes = wlp
            } else {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, CoinEngineFactory.create(card!!.blockchain)!!.getShareWalletUri(card))
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    showSingleToast(R.string.no_compatible_wallet)
                }
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
            if (UtilHelper.isOnline(activity!!)) {
                if (!card!!.hasBalanceInfo()) {
                    showSingleToast(R.string.cannot_obtain_data_from_blockchain)
                } else if (!engine.isBalanceNotZero(card))
                    showSingleToast(R.string.wallet_empty)
                else if (!engine.isBalanceAlterNotZero(card))
                    showSingleToast(R.string.not_enough_eth_for_gas)
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
            } else
                Toast.makeText(activity!!, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
        }

        // request electrum listener
        val electrumBodyListener: ServerApiHelperElectrum.ElectrumRequestDataListener = object : ServerApiHelperElectrum.ElectrumRequestDataListener {
            override fun onElectrumSuccess(electrumRequest: ElectrumRequest?) {
                requestCounter--
                if (requestCounter == 0) srl?.isRefreshing = false

                if (electrumRequest!!.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                    try {
                        val walletAddress = electrumRequest.params.getString(0)
                        val confBalance = electrumRequest.result.getLong("confirmed")
                        val unconfirmedBalance = electrumRequest.result.getLong("unconfirmed")
                        card!!.isBalanceReceived = true
                        card!!.setBalanceConfirmed(confBalance)
                        card!!.balanceUnconfirmed = unconfirmedBalance
                        card!!.decimalBalance = confBalance.toString()
                        card!!.validationNodeDescription = serverApiHelperElectrum.validationNodeDescription
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e(TAG, "FAIL METHOD_GetBalance JSONException")
                    }
                }

                if (electrumRequest.isMethod(ElectrumRequest.METHOD_ListUnspent)) {
                    try {
                        val walletAddress = electrumRequest.params.getString(0)
                        val jsUnspentArray = electrumRequest.resultArray
                        try {
                            card!!.unspentTransactions.clear()
                            for (i in 0 until jsUnspentArray.length()) {
                                val jsUnspent = jsUnspentArray.getJSONObject(i)
                                val trUnspent = TangemCard.UnspentTransaction()
                                trUnspent.txID = jsUnspent.getString("tx_hash")
                                trUnspent.Amount = jsUnspent.getInt("value")
                                trUnspent.Height = jsUnspent.getInt("height")
                                card!!.unspentTransactions.add(trUnspent)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            Log.e(TAG, "FAIL METHOD_ListUnspent JSONException")
                        }

                        for (i in 0 until jsUnspentArray.length()) {
                            val jsUnspent = jsUnspentArray.getJSONObject(i)
                            val height = jsUnspent.getInt("height")
                            val hash = jsUnspent.getString("tx_hash")
                            if (height != -1) {
                                requestElectrum(card!!, ElectrumRequest.getTransaction(walletAddress, hash))
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }

                if (electrumRequest.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
                    try {
                        val txHash = electrumRequest.txHash
                        val raw = electrumRequest.resultString
                        val listTx = card!!.unspentTransactions
                        for (tx in listTx) {
                            if (tx.txID == txHash)
                                tx.Raw = raw
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }

                if (electrumRequest.isMethod(ElectrumRequest.METHOD_SendTransaction)) {

                }

                if (requestCounter == 0)
                    updateViews()
            }

            override fun onElectrumFail(method: String?) {
                srl!!.isRefreshing = false
            }
        }

        serverApiHelperElectrum.setElectrumRequestData(electrumBodyListener)

        // request card verify and get info listener
        val cardVerifyAndGetInfoListener: ServerApiHelper.CardVerifyAndGetInfoListener = object : ServerApiHelper.CardVerifyAndGetInfoListener {
            override fun onSuccess(cardVerifyAndGetArtworkResponse: CardVerifyAndGetInfo.Response?) {
                requestCounter--
                if (requestCounter == 0) srl!!.isRefreshing = false

                val result = cardVerifyAndGetArtworkResponse?.results!![0]
                if (result.error != null) {
                    card!!.isOnlineVerified = false
                    return
                }
                card!!.isOnlineVerified = result.passed

                if (requestCounter == 0) updateViews()

                if (!result.passed) return

                if (localStorage.checkBatchInfoChanged(card!!, result)) {
                    Log.w(TAG, "Batch ${result.batch} info  changed to '$result'")
                    ivTangemCard.setImageBitmap(localStorage.getCardArtworkBitmap(card!!))
                    localStorage.applySubstitution(card!!)
                    if (card!!.blockchain == Blockchain.Token || card!!.blockchain == Blockchain.Ethereum) {
                        card!!.setBlockchainIDFromCard(Blockchain.Ethereum.id)
                    }
                    refresh()
                }
                if (result.artwork != null && localStorage.checkNeedUpdateArtwork(result.artwork)) {
                    Log.w(TAG, "Artwork '${result.artwork!!.id}' updated, need download")
                    serverApiHelper.requestArtwork(result.artwork!!.id, result.artwork!!.getUpdateDate(), card!!)
                    updateViews()
                }
//            Log.i(TAG, "setCardVerify " + it.results!![0].passed)
            }

            override fun onFail(message: String?) {
                requestCounter--
                if (requestCounter == 0)
                    srl!!.isRefreshing = false
            }
        }
        serverApiHelper.setCardVerifyAndGetInfoListener(cardVerifyAndGetInfoListener)

        // request artwork listener
        val artworkListener: ServerApiHelper.ArtworkListener = object : ServerApiHelper.ArtworkListener {
            override fun onSuccess(artworkId: String?, inputStream: InputStream?, updateDate: Date?) {
                localStorage.updateArtwork(artworkId!!, inputStream!!, updateDate!!)
                ivTangemCard.setImageBitmap(localStorage.getCardArtworkBitmap(card!!))
                Log.w(TAG, "Artwork '$artworkId' downloaded")
            }

            override fun onFail(message: String?) {

            }
        }
        serverApiHelper.setArtworkListener(artworkListener)

        // request rate info listener
        serverApiHelper.setRateInfoData {
            requestCounter--
            val rate = it.priceUsd.toFloat()
            card!!.rate = rate
            card!!.rateAlter = rate

            if (requestCounter == 0) {
                srl!!.isRefreshing = false
                updateViews()
            }
        }

        // request eth get balance, eth get transaction count, eth call, eth sendRawTransaction listener
        val infuraBodyListener: ServerApiHelper.InfuraBodyListener = object : ServerApiHelper.InfuraBodyListener {
            override fun onInfuraSuccess(method: String, infuraResponse: InfuraResponse) {
                when (method) {
                    ServerApiHelper.INFURA_ETH_GET_BALANCE -> {
                        var balanceCap = infuraResponse.result
                        balanceCap = balanceCap.substring(2)
                        val l = BigInteger(balanceCap, 16)
                        val d = l.divide(BigInteger("1000000000000000000", 10))
                        val balance = d.toLong()

                        card!!.setBalanceConfirmed(balance)
                        card!!.balanceUnconfirmed = 0L
                        card!!.isBalanceReceived = true
                        if (card!!.blockchain != Blockchain.Token)
                            card!!.decimalBalance = l.toString(10)
                        card!!.decimalBalanceAlter = l.toString(10)

                        Log.i("$TAG eth_get_balance", balanceCap)
                    }

                    ServerApiHelper.INFURA_ETH_GET_TRANSACTION_COUNT -> {
                        var nonce = infuraResponse.result
                        nonce = nonce.substring(2)
                        val count = BigInteger(nonce, 16)
                        card!!.confirmedTXCount = count

//                        Log.i("$TAG eth_getTransCount", nonce)
                    }

                    ServerApiHelper.INFURA_ETH_GET_PENDING_COUNT -> {
                        var pending = infuraResponse.result
                        pending = pending.substring(2)
                        val count = BigInteger(pending, 16)
                        card!!.unconfirmedTXCount = count

//                        Log.i("$TAG eth_getPendingTxCount", pending)
                    }

                    ServerApiHelper.INFURA_ETH_CALL -> {
                        try {
                            var balanceCap = infuraResponse.result
                            balanceCap = balanceCap.substring(2)
                            val l = BigInteger(balanceCap, 16)
                            val balance = l.toLong()
                            if (l.compareTo(BigInteger.ZERO) == 0) {
                                card!!.blockchainID = Blockchain.Ethereum.id
                                card!!.addTokenToBlockchainName()

                                requestCounter--
                                if (requestCounter == 0) srl!!.isRefreshing = false

                                requestInfura(ServerApiHelper.INFURA_ETH_GET_BALANCE, "")
                                requestInfura(ServerApiHelper.INFURA_ETH_GET_TRANSACTION_COUNT, "")
                                requestInfura(ServerApiHelper.INFURA_ETH_GET_PENDING_COUNT, "")
                                return
                            }
                            card!!.setBalanceConfirmed(balance)
                            card!!.balanceUnconfirmed = 0L
                            card!!.decimalBalance = l.toString(10)

//                            Log.i("$TAG eth_call", balanceCap)

                            requestInfura(ServerApiHelper.INFURA_ETH_GET_BALANCE, "")
                            requestInfura(ServerApiHelper.INFURA_ETH_GET_TRANSACTION_COUNT, "")
                            requestInfura(ServerApiHelper.INFURA_ETH_GET_PENDING_COUNT, "")
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    ServerApiHelper.INFURA_ETH_SEND_RAW_TRANSACTION -> {
                        try {
                            var hashTX: String
                            try {
                                val tmp = infuraResponse.result
                                hashTX = tmp
                            } catch (e: JSONException) {
                                return
                            }

                            if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                hashTX = hashTX.substring(2)
                            }

                            Log.e("$TAG TX_RESULT", hashTX)

                            val nonce = card!!.confirmedTXCount
                            nonce.add(BigInteger.valueOf(1))
                            card!!.confirmedTXCount = nonce

                            Log.e("$TAG TX_RESULT", hashTX)

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                requestCounter--
                if (requestCounter == 0) {
                    srl!!.isRefreshing = false
                    updateViews()
                }
            }

            override fun onInfuraFail(method: String, message: String) {
                requestCounter--
                if (requestCounter == 0) {
                    srl!!.isRefreshing = false
                    updateViews()
                }
            }
        }
        serverApiHelper.setInfuraResponse(infuraBodyListener)
    }

    override fun onResume() {
        super.onResume()
        nfcManager!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        nfcManager!!.onPause()
        if (timerRepeatRefresh != null)
            timerRepeatRefresh!!.cancel()
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
            REQUEST_CODE_SEND_PAYMENT, REQUEST_CODE_RECEIVE_PAYMENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    card!!.clearInfo()
                    srl!!.postDelayed({ this.refresh() }, 5000)
                    srl!!.isRefreshing = true
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
        if (rlProgressBar != null)
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
                    if (!cardProtocol.card.isWalletPublicKeyValid) refresh()
                    else updateViews()
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

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

            if (card!!.message == null || card!!.message.isEmpty()) {
                tvMessage!!.text = ""
                tvMessage!!.visibility = View.GONE
            } else {
                tvMessage!!.text = card!!.message
                tvMessage!!.visibility = View.VISIBLE
            }

            val engine = CoinEngineFactory.create(card!!.blockchain)

            if (srl!!.isRefreshing) {
                tvBalanceLine1.setTextColor(resources.getColor(R.color.primary))
                tvBalanceLine1.text = getString(R.string.verifying_in_blockchain)
                tvBalanceLine2.text = ""
            } else {
                val validator = BalanceValidator()
                validator.Check(card, false)
                tvBalanceLine1.setTextColor(ContextCompat.getColor(context!!, validator.color))
                tvBalanceLine1.text = validator.firstLine
                tvBalanceLine2.text = validator.getSecondLine(false)
            }

            if (engine!!.hasBalanceInfo(card) || card!!.offlineBalance == null) {
                if ((card!!.blockchain == Blockchain.Token) && (engine.getBalanceWithAlter(card) != null)) {
                    val html = Html.fromHtml(engine.getBalanceWithAlter(card))
                    tvBalance.text = html
                } else
//                    tvBalance.text = engine.getBalanceWithAlter(card)
                    tvBalance.text = engine.getBalance(card)

            } else {
                val offlineAmount = engine.convertByteArrayToAmount(card, card!!.offlineBalance)
                if (card!!.blockchain == Blockchain.Token) {
                    tvBalance.setText(R.string.not_implemented)
                } else
                    tvBalance.text = engine.getAmountDescription(card, offlineAmount)
            }

            tvWallet.text = card!!.wallet

            tvBlockchain.text = card!!.blockchainName

            if (card!!.hasBalanceInfo() && (card!!.balance != 0L) && (card!!.balance != null)) {
                btnExtract.isEnabled = true
                btnExtract.backgroundTintList = activeColor
            } else {
                btnExtract.isEnabled = false
                btnExtract.backgroundTintList = inactiveColor
            }

            card!!.error = null
            card!!.message = null

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun refresh() {
        if ((srl == null) || (card == null)) return;
        try {
            // clear all card data and request again
            srl!!.isRefreshing = true
            card!!.clearInfo()
            card!!.error = null
            card!!.message = null
            requestCounter = 0

            updateViews()

            val engine = CoinEngineFactory.create(card!!.blockchain)

            requestCardVerify()

            // Bitcoin
            if (card!!.blockchain == Blockchain.Bitcoin || card!!.blockchain == Blockchain.BitcoinTestNet) {
                card!!.setIsBalanceEqual(true)

                requestElectrum(card!!, ElectrumRequest.checkBalance(card!!.wallet))
                requestElectrum(card!!, ElectrumRequest.listUnspent(card!!.wallet))
                requestRateInfo("bitcoin")
            }

            // BitcoinCash
            else if (card!!.blockchain == Blockchain.BitcoinCash || card!!.blockchain == Blockchain.BitcoinCashTestNet) {
                card!!.setIsBalanceEqual(true)

                requestElectrum(card!!, ElectrumRequest.checkBalance(card!!.wallet))
                requestElectrum(card!!, ElectrumRequest.listUnspent(card!!.wallet))
                requestRateInfo("bitcoin-cash")
            }

            // Ethereum
            else if (card!!.blockchain == Blockchain.Ethereum || card!!.blockchain == Blockchain.EthereumTestNet) {
                requestInfura(ServerApiHelper.INFURA_ETH_GET_BALANCE, "")
                requestInfura(ServerApiHelper.INFURA_ETH_GET_TRANSACTION_COUNT, "")
                requestInfura(ServerApiHelper.INFURA_ETH_GET_PENDING_COUNT, "")
                requestRateInfo("ethereum")
            }

            // Token
            else if (card!!.blockchain == Blockchain.Token) {
                requestInfura(ServerApiHelper.INFURA_ETH_CALL, engine.getContractAddress(card))
                requestRateInfo("ethereum")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestElectrum(card: TangemCard, electrumRequest: ElectrumRequest) {
        if (UtilHelper.isOnline(activity!!)) {
            requestCounter++
            serverApiHelperElectrum.electrumRequestData(card, electrumRequest)
        } else {
            Toast.makeText(activity!!, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
            srl!!.isRefreshing = false
        }

    }

    private fun requestInfura(method: String, contract: String) {
        if (UtilHelper.isOnline(activity!!)) {
            requestCounter++
            serverApiHelper.infura(method, 67, card!!.wallet, contract, "")
        } else {
            Toast.makeText(activity!!, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
            srl!!.isRefreshing = false
        }
    }

    private fun requestCardVerify() {
        if (UtilHelper.isOnline(activity!!)) {
            if ((card!!.isOnlineVerified == null || !card!!.isOnlineVerified)) {
                requestCounter++
                serverApiHelper.cardVerifyAndGetInfo(card)
            }
        } else {
            Toast.makeText(activity!!, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
            srl!!.isRefreshing = false
        }
    }

    private fun requestRateInfo(cryptoId: String) {
        if (UtilHelper.isOnline(activity!!)) {
            requestCounter++
            serverApiHelper.rateInfoData(cryptoId)
        } else {
            Toast.makeText(activity!!, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
            srl!!.isRefreshing = false
        }
    }

//    private fun prepareResultIntent(): Intent {
//        val data = Intent()
//        data.putExtra("UID", card!!.uid)
//        data.putExtra("Card", card!!.asBundle)
//        return data
//    }

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

    private fun startSwapPINActivity() {
        val intent = Intent(context, PinSwapActivity::class.java)
        intent.putExtra("UID", card!!.uid)
        intent.putExtra("Card", card!!.asBundle)
        intent.putExtra("newPIN", newPIN)
        intent.putExtra("newPIN2", newPIN2)
        startActivityForResult(intent, REQUEST_CODE_SWAP_PIN)
    }

    var showTime: Date = Date()

    private fun showSingleToast(text: Int) {
        if (singleToast == null || !singleToast!!.view.isShown || showTime.time + 2000 < Date().time) {
            if (singleToast != null)
                singleToast!!.cancel()
            singleToast = Toast.makeText(context, text, Toast.LENGTH_LONG)
            singleToast!!.show()
            showTime = Date()
        }
    }

}