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
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.tangem.App
import com.tangem.Constant
import com.tangem.data.network.ServerApiCommon
import com.tangem.data.network.ServerApiInfura
import com.tangem.tangemserver.android.model.CardVerifyAndGetInfo
import com.tangem.data.network.model.InfuraResponse
import com.tangem.tangemcard.tasks.VerifyCardTask
import com.tangem.tangemcard.reader.CardProtocol
import com.tangem.tangemcard.android.reader.NfcManager
import com.tangem.domain.wallet.*
import com.tangem.domain.wallet.bch.BtcCashEngine
import com.tangem.domain.wallet.eth.EthData
import com.tangem.domain.wallet.token.TokenData
import com.tangem.domain.wallet.token.TokenEngine
import com.tangem.presentation.activity.*
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.PINSwapWarningDialog
import com.tangem.presentation.dialog.ShowQRCodeDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.data.Blockchain
import com.tangem.data.network.ElectrumRequest
import com.tangem.tangemcard.android.reader.NfcReader
import com.tangem.tangemcard.data.EXTRA_TANGEM_CARD
import com.tangem.tangemcard.data.EXTRA_TANGEM_CARD_UID
import com.tangem.tangemcard.data.TangemCard
import com.tangem.tangemcard.data.loadFromBundle
import com.tangem.tangemcard.util.Util
import com.tangem.tangemserver.android.ServerApiTangem
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fr_loaded_wallet.*
import org.json.JSONException
import java.io.InputStream
import java.math.BigInteger
import java.util.*

class LoadedWallet : Fragment(), NfcAdapter.ReaderCallback, CardProtocol.Notifications, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        val TAG: String = LoadedWallet::class.java.simpleName
    }

    private lateinit var nfcManager: NfcManager

    private var serverApiCommon: ServerApiCommon = ServerApiCommon()
    private var serverApiTangem: ServerApiTangem = ServerApiTangem()

    private var singleToast: Toast? = null
    private lateinit var ctx: TangemContext

    private var lastTag: Tag? = null
    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0
    private var timerHideErrorAndMessage: Timer? = null
    private var newPIN = ""
    private var newPIN2 = ""
    private var cardProtocol: CardProtocol? = null
    private val inactiveColor: ColorStateList by lazy { resources.getColorStateList(R.color.btn_dark) }
    private val activeColor: ColorStateList by lazy { resources.getColorStateList(R.color.colorAccent) }
    private var requestCounter = 0
    private var timerRepeatRefresh: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcManager = NfcManager(activity, this)

        ctx = TangemContext.loadFromBundle(activity, activity?.intent?.extras)

        lastTag = activity?.intent?.getParcelableExtra(Constant.EXTRA_LAST_DISCOVERED_TAG)

        //localStorage = activity?.let { CardDataSubstitutionProvider(it) }!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fr_loaded_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ctx.blockchain == Blockchain.Token)
            tvBalance.setSingleLine(false)

        ivTangemCard.setImageBitmap(App.localStorage.getCardArtworkBitmap(ctx.card))

        btnExtract.isEnabled = false
        btnExtract.backgroundTintList = inactiveColor

        refresh()

        startVerify(lastTag)

        tvWallet.text = ctx.coinData.wallet

        // set listeners
        srl.setOnRefreshListener { refresh() }
        btnLookup.setOnClickListener {
            val engine = CoinEngineFactory.create(ctx)
            val browserIntent = Intent(Intent.ACTION_VIEW, engine?.shareWalletUriExplorer)
            startActivity(browserIntent)
        }
        btnCopy.setOnClickListener { doShareWallet(false) }
        tvWallet.setOnClickListener { doShareWallet(false) }
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
                                val engine = CoinEngineFactory.create(ctx)
                                val intent = Intent(Intent.ACTION_VIEW, engine!!.shareWalletUri)
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
                            val engine = CoinEngineFactory.create(ctx)
                            ShowQRCodeDialog.show(activity, engine!!.shareWalletUri.toString())
                        }
//                        getString(R.string.via_cryptonit2) -> {
//                            val intent = Intent(ctx, PrepareCryptonitOtherApiWithdrawalActivity::class.java)
//                            intent.putExtra("UID", card!!.uid)
//                            intent.putExtra("Card", card!!.asBundle)
//                            startActivityForResult(intent, REQUEST_CODE_RECEIVE_PAYMENT)
//                        }
                        getString(R.string.via_cryptonit) -> {
                            val intent = Intent(activity, PrepareCryptonitWithdrawalActivity::class.java)
                            ctx.saveToIntent(intent)
                            startActivityForResult(intent, Constant.REQUEST_CODE_RECEIVE_PAYMENT)
                        }
                        getString(R.string.via_kraken) -> {
                            val intent = Intent(activity, PrepareKrakenWithdrawalActivity::class.java)
                            ctx.saveToIntent(intent)
                            startActivityForResult(intent, Constant.REQUEST_CODE_RECEIVE_PAYMENT)
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
                    val engine = CoinEngineFactory.create(ctx)
                    val intent = Intent(Intent.ACTION_VIEW, engine!!.shareWalletUri)
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    showSingleToast(R.string.no_compatible_wallet)
                }
            }
        }
        btnDetails.setOnClickListener {
            if (cardProtocol != null)
//                openVerifyCard(cardProtocol!!)
                (activity as LoadedWalletActivity).navigator.showVerifyCard(context as Activity, ctx)
            else
                showSingleToast(R.string.need_attach_card_again)
        }
        btnScanAgain.setOnClickListener { (activity as LoadedWalletActivity).navigator.showMain(context as Activity) }
        btnExtract.setOnClickListener {
            val engine = CoinEngineFactory.create(ctx)
            if (UtilHelper.isOnline(context as Activity)) {
                if (!engine!!.isExtractPossible)
                    showSingleToast(ctx.message)
                else if (ctx.card!!.remainingSignatures == 0)
                    showSingleToast(R.string.card_has_no_remaining_signature)
                else {
                    val intent = Intent(activity, PreparePaymentActivity::class.java)
                    ctx.saveToIntent(intent)
                    startActivityForResult(intent, Constant.REQUEST_CODE_SEND_PAYMENT)
                }
            } else
                Toast.makeText(activity, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
        }

        // request electrum listener
//        val electrumBodyListener: ServerApiElectrum.ElectrumRequestDataListener = object : ServerApiElectrum.ElectrumRequestDataListener {
//            override fun onSuccess(electrumRequest: ElectrumRequest?) {
//                if (electrumRequest!!.isMethod(ElectrumRequest.METHOD_GetBalance)) {
//                    try {
//                        val walletAddress = electrumRequest.params.getString(0)
//                        val confBalance = electrumRequest.result.getLong("confirmed")
//                        val unconfirmedBalance = electrumRequest.result.getLong("unconfirmed")
//                        ctx.coinData!!.isBalanceReceived = true
//                        (ctx.coinData!! as BtcData).setBalanceConfirmed(confBalance)
//                        (ctx.coinData!! as BtcData).balanceUnconfirmed = unconfirmedBalance
//                        (ctx.coinData!! as BtcData).validationNodeDescription = serverApiElectrum.validationNodeDescription
//                    } catch (e: JSONException) {
//                        e.printStackTrace()
//                        Log.e(TAG, "FAIL METHOD_GetBalance JSONException")
//                    }
//                }
//
//                if (electrumRequest.isMethod(ElectrumRequest.METHOD_ListUnspent)) {
//                    try {
//                        val walletAddress = electrumRequest.params.getString(0)
//                        val jsUnspentArray = electrumRequest.resultArray
//                        try {
//                            (ctx.coinData!! as BtcData).unspentTransactions.clear()
//                            for (i in 0 until jsUnspentArray.length()) {
//                                val jsUnspent = jsUnspentArray.getJSONObject(i)
//                                val trUnspent = BtcData.UnspentTransaction()
//                                trUnspent.txID = jsUnspent.getString("tx_hash")
//                                trUnspent.Amount = jsUnspent.getInt("value")
//                                trUnspent.Height = jsUnspent.getInt("height")
//                                (ctx.coinData!! as BtcData).unspentTransactions.add(trUnspent)
//                            }
//                        } catch (e: JSONException) {
//                            e.printStackTrace()
//                            Log.e(TAG, "FAIL METHOD_ListUnspent JSONException")
//                        }
//
//                        for (i in 0 until jsUnspentArray.length()) {
//                            val jsUnspent = jsUnspentArray.getJSONObject(i)
//                            val height = jsUnspent.getInt("height")
//                            val hash = jsUnspent.getString("tx_hash")
//                            if (height != -1) {
//                                requestElectrum(ElectrumRequest.getTransaction(walletAddress, hash))
//                            }
//                        }
//                    } catch (e: JSONException) {
//                        e.printStackTrace()
//                    }
//                }
//
//                if (electrumRequest.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
//                    try {
//                        val txHash = electrumRequest.txHash
//                        val raw = electrumRequest.resultString
//                        val listTx = (ctx.coinData!! as BtcData).unspentTransactions
//                        for (tx in listTx) {
//                            if (tx.txID == txHash)
//                                tx.Raw = raw
//                        }
//                    } catch (e: JSONException) {
//                        e.printStackTrace()
//                    }
//                }
//
//                if (electrumRequest.isMethod(ElectrumRequest.METHOD_SendTransaction)) {
//
//                }
//
//                counterMinus()
//            }
//
//            override fun onFail(method: String?) {
//
//            }
//        }
//        serverApiElectrum.setElectrumRequestData(electrumBodyListener)

//        // request infura listener
//        val infuraBodyListener: ServerApiInfura.InfuraBodyListener = object : ServerApiInfura.InfuraBodyListener {
//            override fun onSuccess(method: String, infuraResponse: InfuraResponse) {
//                when (method) {
//                    ServerApiInfura.INFURA_ETH_GET_BALANCE -> {
//                        var balanceCap = infuraResponse.result
//                        balanceCap = balanceCap.substring(2)
//                        val l = BigInteger(balanceCap, 16)
////                        val d = l.divide(BigInteger("1000000000000000000", 10))
////                        val balance = d.toLong()
//
////                        (ctx.coinData!! as EthData).setBalanceConfirmed(balance)
////                        (ctx.coinData!! as EthData).balanceUnconfirmed = 0L
//                        if (ctx.blockchain != Blockchain.Token) {
//                            (ctx.coinData!! as EthData).isBalanceReceived = true
//                            (ctx.coinData!! as EthData).balanceInInternalUnits = CoinEngine.InternalAmount(l.toBigDecimal(), "wei")
//                        } else {
//                            (ctx.coinData!! as TokenData).isBalanceReceived = true
//                            //(ctx.coinData!! as TokenData).balanceInInternalUnits = CoinEngine.InternalAmount(l.toBigDecimal(),ctx.card.tokenSymbol)
//                            (ctx.coinData!! as TokenData).balanceAlterInInternalUnits = CoinEngine.InternalAmount(l.toBigDecimal(), "wei")
//                        }
//
////                        Log.i("$TAG eth_get_balance", balanceCap)
//                    }
//
//                    ServerApiInfura.INFURA_ETH_GET_TRANSACTION_COUNT -> {
//                        var nonce = infuraResponse.result
//                        nonce = nonce.substring(2)
//                        val count = BigInteger(nonce, 16)
//                        (ctx.coinData!! as EthData).confirmedTXCount = count
//
//
////                        Log.i("$TAG eth_getTransCount", nonce)
//                    }
//
//                    ServerApiInfura.INFURA_ETH_GET_PENDING_COUNT -> {
//                        var pending = infuraResponse.result
//                        pending = pending.substring(2)
//                        val count = BigInteger(pending, 16)
//                        (ctx.coinData!! as EthData).unconfirmedTXCount = count
//
////                        Log.i("$TAG eth_getPendingTxCount", pending)
//                    }
//
//                    ServerApiInfura.INFURA_ETH_CALL -> {
//                        try {
//                            var balanceCap = infuraResponse.result
//                            balanceCap = balanceCap.substring(2)
//                            val l = BigInteger(balanceCap, 16)
//                            val balance = l.toLong()
////                            if (l.compareTo(BigInteger.ZERO) == 0) {
////                                //ctx.card!!.blockchainID = Blockchain.Ethereum.id
////                                ctx.card!!.addTokenToBlockchainName()
////
////                                //TODO check
////                                //ctx.blockchain=lBlockchain.Ethereum
////
////                                requestCounter--
////                                if (requestCounter == 0) srl!!.isRefreshing = false
////
////                                requestInfura(ServerApiCommon.INFURA_ETH_GET_BALANCE, "")
////                                requestInfura(ServerApiCommon.INFURA_ETH_GET_TRANSACTION_COUNT, "")
////                                requestInfura(ServerApiCommon.INFURA_ETH_GET_PENDING_COUNT, "")
////                                return
////                            }
//                            (ctx.coinData!! as EthData).balanceInInternalUnits = CoinEngine.InternalAmount(l.toBigDecimal(), ctx.card.tokenSymbol)
//
////                            Log.i("$TAG eth_call", balanceCap)
//
//                            requestInfura(ServerApiInfura.INFURA_ETH_GET_BALANCE, "")
//                            requestInfura(ServerApiInfura.INFURA_ETH_GET_TRANSACTION_COUNT, "")
//                            requestInfura(ServerApiInfura.INFURA_ETH_GET_PENDING_COUNT, "")
//                        } catch (e: JSONException) {
//                            e.printStackTrace()
//                        } catch (e: NumberFormatException) {
//                            e.printStackTrace()
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
//                    }
//
//                    ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION -> {
//                        try {
//                            var hashTX: String
//                            try {
//                                val tmp = infuraResponse.result
//                                hashTX = tmp
//                            } catch (e: JSONException) {
//                                return
//                            }
//
//                            if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
//                                hashTX = hashTX.substring(2)
//                            }
//
//                            Log.e("$TAG TX_RESULT", hashTX)
//
//                            val nonce = (ctx.coinData!! as EthData).confirmedTXCount
//                            nonce.add(BigInteger.valueOf(1))
//                            (ctx.coinData!! as EthData).confirmedTXCount = nonce
//
//                            Log.e("$TAG TX_RESULT", hashTX)
//
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
//                    }
//                }
//
//                counterMinus()
//            }
//
//            override fun onFail(method: String, message: String) {
//
//            }
//        }
//        serverApiInfura.setInfuraResponse(infuraBodyListener)

        // request card verify and get info listener
        val cardVerifyAndGetInfoListener: ServerApiTangem.CardVerifyAndGetInfoListener = object : ServerApiTangem.CardVerifyAndGetInfoListener {
            override fun onSuccess(cardVerifyAndGetArtworkResponse: CardVerifyAndGetInfo.Response?) {
                val result = cardVerifyAndGetArtworkResponse?.results!![0]
                if (result.error != null) {
                    ctx.card!!.isOnlineVerified = false
                    return
                }
                ctx.card!!.isOnlineVerified = result.passed

                if (requestCounter == 0) updateViews()

                if (!result.passed) return

                if (App.localStorage.checkBatchInfoChanged(ctx.card!!, result)) {
                    Log.w(TAG, "Batch ${result.batch} info  changed to '$result'")
                    ivTangemCard.setImageBitmap(App.localStorage.getCardArtworkBitmap(ctx.card!!))
                    App.localStorage.applySubstitution(ctx.card!!)
                    //todo - check this is not need after refactoring
//                    if (ctx.blockchain == Blockchain.Token || ctx.blockchain == Blockchain.Ethereum) {
//                        ctx.card!!.setBlockchainIDFromCard(Blockchain.Ethereum.id)

                    //ctx.blockchain=Blockchain.Ethereum
                    //engine=engine!!.swithToOtherEngine(Blockchain.Ethereum)
//                    }
                    refresh()
                }
                if (result.artwork != null && App.localStorage.checkNeedUpdateArtwork(result.artwork)) {
                    Log.w(TAG, "Artwork '${result.artwork!!.id}' updated, need download")
                    serverApiTangem.requestArtwork(result.artwork!!.id, result.artwork!!.getUpdateDate(), ctx.card!!)
                    updateViews()
                }
//            Log.i(TAG, "setCardVerify " + it.results!![0].passed)
            }

            override fun onFail(message: String?) {

            }
        }
        serverApiTangem.setCardVerifyAndGetInfoListener(cardVerifyAndGetInfoListener)

        // request artwork listener
        val artworkListener: ServerApiTangem.ArtworkListener = object : ServerApiTangem.ArtworkListener {
            override fun onSuccess(artworkId: String?, inputStream: InputStream?, updateDate: Date?) {
                App.localStorage.updateArtwork(artworkId!!, inputStream!!, updateDate!!)
                ivTangemCard.setImageBitmap(App.localStorage.getCardArtworkBitmap(ctx.card!!))
            }

            override fun onFail(message: String?) {

            }
        }
        serverApiTangem.setArtworkListener(artworkListener)

        // request rate info listener
        serverApiCommon.setRateInfoData {
            val rate = it.priceUsd.toFloat()
            ctx.coinData!!.rate = rate
            ctx.coinData!!.rateAlter = rate
        }
    }

    private fun counterMinus() {
        requestCounter--
        if (requestCounter == 0) {
            if (srl != null) srl!!.isRefreshing = false
            updateViews()
        }
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
            Constant.REQUEST_CODE_VERIFY_CARD ->
                // action when erase wallet
                if (resultCode == Activity.RESULT_OK) {
                    if (activity != null)
                        activity?.finish()
                }

            Constant.REQUEST_CODE_ENTER_NEW_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.extras != null && data.extras!!.containsKey("confirmPIN")) {
                        val intent = Intent(activity, PinRequestActivity::class.java)
                        intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                        ctx.saveToIntent(intent)
                        newPIN = data.getStringExtra("newPIN")
                        startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(activity, PinRequestActivity::class.java)
                        intent.putExtra("newPIN", data.getStringExtra("newPIN"))
                        intent.putExtra("mode", PinRequestActivity.Mode.ConfirmNewPIN.toString())
                        startActivityForResult(intent, Constant.REQUEST_CODE_ENTER_NEW_PIN)
                    }
                }
            }
            Constant.REQUEST_CODE_ENTER_NEW_PIN2 -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.extras != null && data.extras!!.containsKey("confirmPIN2")) {
                        val intent = Intent(activity, PinRequestActivity::class.java)
                        intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                        ctx.saveToIntent(intent)
                        newPIN2 = data.getStringExtra("newPIN2")
                        startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(activity, PinRequestActivity::class.java)
                        intent.putExtra("newPIN2", data.getStringExtra("newPIN2"))
                        intent.putExtra("mode", PinRequestActivity.Mode.ConfirmNewPIN2.toString())
                        startActivityForResult(intent, Constant.REQUEST_CODE_ENTER_NEW_PIN2)
                    }
                }
            }
            Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (newPIN == "")
                    newPIN = ctx.card!!.pin

                if (newPIN2 == "")
                    newPIN2 = App.pinStorage.piN2

                val pinSwapWarningDialog = PINSwapWarningDialog()
                pinSwapWarningDialog.setOnRefreshPage { (activity as LoadedWalletActivity).navigator.showPinSwap(context as Activity, newPIN, newPIN2) }
                val bundle = Bundle()
                if (!CardProtocol.isDefaultPIN(newPIN) || !CardProtocol.isDefaultPIN2(newPIN2))
                    bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_forget))
                else
                    bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_use_default))
                pinSwapWarningDialog.arguments = bundle
                pinSwapWarningDialog.show(activity?.supportFragmentManager, PINSwapWarningDialog.TAG)
            }

            Constant.REQUEST_CODE_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    data = Intent()
                    ctx.saveToIntent(data)
                    data.putExtra(Constant.EXTRA_MODIFICATION, "delete")
                } else
                    data.putExtra(Constant.EXTRA_MODIFICATION, "update")

                activity?.setResult(Activity.RESULT_OK, data)
                activity?.finish()

            } else {
                if (data != null && data.extras != null && data.extras!!.containsKey(EXTRA_TANGEM_CARD_UID) && data.extras!!.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getStringExtra(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundleExtra(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(activity, PinRequestActivity::class.java)
                    intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                    ctx.saveToIntent(intent)
                    startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey("message")) {
                        ctx.error = data.getStringExtra("message")
                    }
                }
            }
            Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE -> if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(activity, PurgeActivity::class.java)
                ctx.saveToIntent(intent)
                startActivityForResult(intent, Constant.REQUEST_CODE_PURGE)
            }
            Constant.REQUEST_CODE_PURGE -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    data = Intent()
                    ctx.saveToIntent(data)
                    data.putExtra(Constant.EXTRA_MODIFICATION, "delete")
                } else {
                    data.putExtra(Constant.EXTRA_MODIFICATION, "update")
                }

                activity?.setResult(Activity.RESULT_OK, data)
                activity?.finish()

            } else {
                if (data != null && data.extras != null && data.extras!!.containsKey(EXTRA_TANGEM_CARD_UID) && data.extras!!.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getStringExtra(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundleExtra(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(activity, PinRequestActivity::class.java)
                    intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                    ctx.saveToIntent(intent)
                    startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey("message")) {
                        ctx.error = data.getStringExtra("message")
                    }
                }
                updateViews()
            }
            Constant.REQUEST_CODE_SEND_PAYMENT, Constant.REQUEST_CODE_RECEIVE_PAYMENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    ctx.coinData?.clearInfo()
                    srl?.postDelayed({ this.refresh() }, 5000)
                    srl?.isRefreshing = true
                    updateViews()
                }

                if (data != null && data.extras != null) {
                    if (data.extras!!.containsKey(EXTRA_TANGEM_CARD_UID) && data.extras!!.containsKey(EXTRA_TANGEM_CARD)) {
                        val updatedCard = TangemCard(data.getStringExtra(EXTRA_TANGEM_CARD_UID))
                        updatedCard.loadFromBundle(data.getBundleExtra(EXTRA_TANGEM_CARD))
                        ctx.card = updatedCard
                    }
                    if (data.extras!!.containsKey("message")) {
                        if (resultCode == Activity.RESULT_OK) {
                            ctx.message = data.getStringExtra("message")
                        } else {
                            ctx.error = data.getStringExtra("message")
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

                rlProgressBar?.post {
                    rlProgressBar?.visibility = View.GONE
                    this.cardProtocol = cardProtocol
                    if (!cardProtocol.card.isWalletPublicKeyValid) refresh()
                    else updateViews()
                }
            } else {
                // remove last UIDs because of error and no card read
                rlProgressBar?.post {
                    lastReadSuccess = false
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported)
                        if (!NoExtendedLengthSupportDialog.allReadyShowed)
                            NoExtendedLengthSupportDialog().show(activity?.supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                        else
                            Toast.makeText(activity, R.string.try_to_scan_again, Toast.LENGTH_SHORT).show()
                }
            }
        }

        rlProgressBar?.postDelayed({
            try {
                rlProgressBar?.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadCancel() {
        verifyCardTask = null
        rlProgressBar?.postDelayed({
            try {
                rlProgressBar?.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.OnReadWait(activity, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(activity, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(activity)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

    }

    fun updateViews() {
        if (timerHideErrorAndMessage != null) {
            timerHideErrorAndMessage!!.cancel()
            timerHideErrorAndMessage = null
        }

        if (ctx.error == null || ctx.error.isEmpty()) {
            tvError.visibility = View.GONE
            tvError.text = ""
        } else {
            tvError.visibility = View.VISIBLE
            tvError.text = ctx.error
        }

        if (ctx.message == null || ctx.message.isEmpty()) {
            tvMessage.text = ""
            tvMessage.visibility = View.GONE
        } else {
            tvMessage.text = ctx.message
            tvMessage.visibility = View.VISIBLE
        }

        if (srl!!.isRefreshing) {
            tvBalanceLine1.setTextColor(resources.getColor(R.color.primary))
            tvBalanceLine1.text = getString(R.string.verifying_in_blockchain)
            tvBalanceLine2.text = ""
            tvBalance.text = ""
            tvBalanceEquivalent.text = ""
        } else {
            val validator = BalanceValidator()
            // TODO why attest=false?
            validator.Check(ctx, false)
            context?.let { ContextCompat.getColor(it, validator.color) }?.let { tvBalanceLine1.setTextColor(it) }
            tvBalanceLine1.text = validator.firstLine
            tvBalanceLine2.text = validator.getSecondLine(false)
        }

        val engine = CoinEngineFactory.create(ctx)
        when {
            engine!!.hasBalanceInfo() -> {
                val html = Html.fromHtml(engine.balanceHTML)
                tvBalance.text = html
                tvBalanceEquivalent.text = engine.balanceEquivalent
            }
            ctx.card!!.offlineBalance != null -> {
                val html = Html.fromHtml(engine.offlineBalanceHTML)
                tvBalance.text = html
            }
            else -> tvBalance.text = getString(R.string.no_data_string)
        }

        tvWallet.text = ctx.coinData!!.wallet

        if (ctx.card!!.tokenSymbol.length > 1) {
            val html = Html.fromHtml(ctx.blockchainName)
            tvBlockchain.text = html
        } else
            tvBlockchain.text = ctx.blockchainName

        if (engine.hasBalanceInfo()) {
            btnExtract.isEnabled = true
            btnExtract.backgroundTintList = activeColor
        } else {
            btnExtract.isEnabled = false
            btnExtract.backgroundTintList = inactiveColor
        }

        ctx.error = null
        ctx.message = null
    }

    private fun refresh() {
        if (ctx.card == null) return

        // clear all card data and request again
        srl?.isRefreshing = true
        ctx.coinData.clearInfo()
        ctx.error = null
        ctx.message = null
        requestCounter = 0

        updateViews()

        requestVerifyAndGetInfo()

        if (ctx.blockchain == Blockchain.Bitcoin || ctx.blockchain == Blockchain.BitcoinTestNet || ctx.blockchain == Blockchain.BitcoinCash) {
            val coinEngine = CoinEngineFactory.create(ctx)
            requestCounter++
            coinEngine!!.requestBalanceAndUnspentTransactions(
                    object : CoinEngine.BlockchainRequestsNotifications {
                        override fun onComplete(success: Boolean?) {
                            counterMinus()
                            updateViews()
                        }

                        override fun needTerminate(): Boolean {
                            return !UtilHelper.isOnline(context as Activity)
                        }
                    }
            )

        }

        // Bitcoin
        if (ctx.blockchain == Blockchain.Bitcoin || ctx.blockchain == Blockchain.BitcoinTestNet) {
            ctx.coinData.setIsBalanceEqual(true)

//            requestElectrum(ElectrumRequest.checkBalance(ctx.coinData!!.wallet))
//            requestElectrum(ElectrumRequest.listUnspent(ctx.coinData!!.wallet))
            requestRateInfo("bitcoin")
        }

        // BitcoinCash
        else if (ctx.blockchain == Blockchain.BitcoinCash) {
            ctx.coinData.setIsBalanceEqual(true)
//            val engine = CoinEngineFactory.create(ctx)
//
//            requestElectrum(ElectrumRequest.checkBalance((engine as BtcCashEngine).convertToLegacyAddress(ctx.coinData!!.wallet)))
//            requestElectrum(ElectrumRequest.listUnspent(engine.convertToLegacyAddress(ctx.coinData!!.wallet)))
            requestRateInfo("bitcoin-cash")
        }

        // Ethereum
        else if (ctx.blockchain == Blockchain.Ethereum || ctx.blockchain == Blockchain.EthereumTestNet) {
//            requestInfura(ServerApiInfura.INFURA_ETH_GET_BALANCE, "")
//            requestInfura(ServerApiInfura.INFURA_ETH_GET_TRANSACTION_COUNT, "")
//            requestInfura(ServerApiInfura.INFURA_ETH_GET_PENDING_COUNT, "")
            requestRateInfo("ethereum")
        }

        // Token
        else if (ctx.blockchain == Blockchain.Token) {
            val engine = CoinEngineFactory.create(ctx)
//            requestInfura(ServerApiInfura.INFURA_ETH_CALL, (engine as TokenEngine).getContractAddress(ctx.card))
            requestRateInfo("ethereum")
        }
    }

//    private fun requestElectrum(electrumRequest: ElectrumRequest) {
//        if (UtilHelper.isOnline(context as Activity)) {
//            requestCounter++
//            serverApiElectrum.electrumRequestData(ctx, electrumRequest)
//        } else {
//            Toast.makeText(activity, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
//            srl?.isRefreshing = false
//        }
//    }

    private fun requestInfura(method: String, contract: String) {
        if (UtilHelper.isOnline(context as Activity)) {
            requestCounter++
            serverApiInfura.infura(method, 67, ctx.coinData!!.wallet, contract, "")
        } else {
            Toast.makeText(activity, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
            srl?.isRefreshing = false
        }
    }

    private fun requestVerifyAndGetInfo() {
        if (UtilHelper.isOnline(context as Activity)) {
            if ((ctx.card!!.isOnlineVerified == null || !ctx.card!!.isOnlineVerified)) {
                serverApiTangem.cardVerifyAndGetInfo(ctx.card)
            }
        } else {
            Toast.makeText(activity, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
            srl?.isRefreshing = false
        }
    }

    private fun requestRateInfo(cryptoId: String) {
        if (UtilHelper.isOnline(context as Activity)) {
            serverApiCommon.rateInfoData(cryptoId)
        } else {
            Toast.makeText(activity, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
            srl?.isRefreshing = false
        }
    }

    private fun startVerify(tag: Tag?) {
        try {
            val isoDep = IsoDep.get(tag)
                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))
            val uid = tag!!.id
            val sUID = Util.byteArrayToHexString(uid)
            if (ctx.card.uid != sUID) {
//                Log.d(TAG, "Invalid UID: $sUID")
                nfcManager!!.ignoreTag(isoDep.tag)
                return
            } else {
//                Log.v(TAG, "UID: $sUID")
            }

            if (lastReadSuccess)
                isoDep.timeout = 1000
            else
                isoDep.timeout = 65000

            verifyCardTask = VerifyCardTask(ctx.card, NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, App.firmwaresStorage, this)
            verifyCardTask?.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doShareWallet(useURI: Boolean) {
        if (useURI) {
            val engine = CoinEngineFactory.create(ctx)
            val txtShare = engine!!.shareWalletUri.toString()
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, "Wallet address")
            intent.putExtra(Intent.EXTRA_TEXT, txtShare)

            val packageManager = activity?.packageManager
            val activities = packageManager?.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            val isIntentSafe = activities?.size!! > 0

            if (isIntentSafe) {
                // create intent to show chooser
                val chooser = Intent.createChooser(intent, getString(R.string.share_wallet_address_with))

                // verify the intent will resolve to at least one activity
                if (intent.resolveActivity(activity!!.packageManager) != null) {
                    startActivity(chooser)
                }
            } else {
                val clipboard = activity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip = ClipData.newPlainText(txtShare, txtShare)
                Toast.makeText(activity, R.string.copied_clipboard, Toast.LENGTH_LONG).show()
            }
        } else {
            val txtShare = ctx.coinData.wallet
            val clipboard = activity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = ClipData.newPlainText(txtShare, txtShare)
            Toast.makeText(activity, R.string.copied_clipboard, Toast.LENGTH_LONG).show()
        }
    }

    private var showTime: Date = Date()

    private fun showSingleToast(text: Int) {
        if (singleToast == null || !singleToast!!.view.isShown || showTime.time + 2000 < Date().time) {
            if (singleToast != null)
                singleToast!!.cancel()
            singleToast = Toast.makeText(activity, text, Toast.LENGTH_LONG)
            singleToast!!.show()
            showTime = Date()
        }
    }

    private fun showSingleToast(text: String) {
        if (singleToast == null || !singleToast!!.view.isShown || showTime.time + 2000 < Date().time) {
            if (singleToast != null)
                singleToast!!.cancel()
            singleToast = Toast.makeText(activity, text, Toast.LENGTH_LONG)
            singleToast!!.show()
            showTime = Date()
        }
    }

}