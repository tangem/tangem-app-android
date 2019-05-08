package com.tangem.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tangem.App
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.data.network.ServerApiCommon
import com.tangem.wallet.BalanceValidator
import com.tangem.wallet.CoinEngine
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.TangemContext
import com.tangem.ui.activity.LoadedWalletActivity
import com.tangem.ui.activity.PinRequestActivity
import com.tangem.ui.activity.PrepareCryptonitWithdrawalActivity
import com.tangem.ui.activity.PrepareKrakenWithdrawalActivity
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.PINSwapWarningDialog
import com.tangem.ui.dialog.ShowQRCodeDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.ui.event.DeletingWalletFinish
import com.tangem.ui.event.TransactionFinishWithError
import com.tangem.ui.event.TransactionFinishWithSuccess
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.card_android.android.reader.NfcReader
import com.tangem.card_android.data.EXTRA_TANGEM_CARD
import com.tangem.card_android.data.EXTRA_TANGEM_CARD_UID
import com.tangem.card_android.data.loadFromBundle
import com.tangem.card_common.data.TangemCard
import com.tangem.card_common.reader.CardProtocol
import com.tangem.card_common.tasks.VerifyCardTask
import com.tangem.card_common.util.Util
import com.tangem.server_android.ServerApiTangem
import com.tangem.server_android.model.CardVerifyAndGetInfo
import com.tangem.util.LOG
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fr_loaded_wallet.*
import kotlinx.android.synthetic.main.layout_btn_details.*
import kotlinx.android.synthetic.main.layout_tangem_card.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.InputStream
import java.util.*
import kotlin.concurrent.timerTask

class LoadedWalletFragment : androidx.fragment.app.Fragment(), NfcAdapter.ReaderCallback, CardProtocol.Notifications, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        val TAG: String = LoadedWalletFragment::class.java.simpleName
    }

    private lateinit var viewModel: LoadedWalletViewModel
    private lateinit var ctx: TangemContext
    private lateinit var nfcManager: NfcManager
    private lateinit var mpSecondScanSound: MediaPlayer
    private var serverApiCommon: ServerApiCommon = ServerApiCommon()
    private var serverApiTangem: ServerApiTangem = ServerApiTangem()
    private var lastTag: Tag? = null
    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0
    private var timerHideErrorAndMessage: Timer? = null
    private var newPIN = ""
    private var newPIN2 = ""
    private var cardProtocol: CardProtocol? = null
    private val inactiveColor: ColorStateList by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            resources.getColorStateList(R.color.btn_dark, activity?.theme)
        else
            @Suppress("DEPRECATION")
            resources.getColorStateList(R.color.btn_dark)
    }
    private val activeColor: ColorStateList by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            resources.getColorStateList(R.color.colorAccent, activity?.theme)
        else
            @Suppress("DEPRECATION")
            resources.getColorStateList(R.color.colorAccent)
    }
    private var requestCounter: Int = 0
        set(value) {
            field = value
            LOG.i(TAG, "requestCounter, set $field")
            if (field <= 0) {
                LOG.e(TAG, "+++++++++++ FINISH REFRESH")
                if (srl != null && srl.isRefreshing)
                    srl.isRefreshing = false
            } else if (srl != null && !srl.isRefreshing)
                srl.isRefreshing = true
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = TangemContext.loadFromBundle(activity, activity?.intent?.extras)

        nfcManager = NfcManager(activity!!, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        lastTag = activity?.intent?.getParcelableExtra(Constant.EXTRA_LAST_DISCOVERED_TAG)

        mpSecondScanSound = MediaPlayer.create(activity, R.raw.scan_card_sound)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fr_loaded_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val engine = CoinEngineFactory.create(ctx)

        tvBalance.setSingleLine(!engine!!.needMultipleLinesForBalance())

        ivTangemCard.setImageBitmap(App.localStorage.getCardArtworkBitmap(ctx.card))

        btnExtract.isEnabled = false
        btnExtract.backgroundTintList = inactiveColor

        tvWallet.text = ctx.coinData.wallet

        // set listeners
        srl.setOnRefreshListener { refresh() }

        tvWallet.setOnClickListener { doShareWallet(false) }

        btnExplore.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, engine.walletExplorerUri)) }

        btnCopy.setOnClickListener { doShareWallet(false) }

        btnLoad.setOnClickListener {
            val items = arrayOf<CharSequence>(getString(R.string.in_app), getString(R.string.load_via_share_address), getString(R.string.load_via_qr))//, getString(R.string.via_cryptonit), getString(R.string.via_kraken))
            val cw = android.view.ContextThemeWrapper(activity, R.style.AlertDialogTheme)
            val dialog = AlertDialog.Builder(cw).setItems(items
            ) { _, which ->
                when (items[which]) {
                    getString(R.string.in_app) -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, engine.shareWalletUri)
                            intent.addCategory(Intent.CATEGORY_DEFAULT)
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            (activity as LoadedWalletActivity).toastHelper.showSingleToast(context, getString(R.string.no_compatible_wallet))
                        }
                    }

                    getString(R.string.load_via_share_address) -> {
                        doShareWallet(true)
                    }

                    getString(R.string.load_via_qr) -> {
                        ShowQRCodeDialog.show(activity as AppCompatActivity?, engine.shareWalletUri.toString())
                    }

                    getString(R.string.via_cryptonit) -> {
                        val intent = Intent(activity, PrepareCryptonitWithdrawalActivity::class.java)
                        ctx.saveToIntent(intent)
                        startActivityForResult(intent, Constant.REQUEST_CODE_RECEIVE_TRANSACTION)
                    }

                    getString(R.string.via_kraken) -> {
                        val intent = Intent(activity, PrepareKrakenWithdrawalActivity::class.java)
                        ctx.saveToIntent(intent)
                        startActivityForResult(intent, Constant.REQUEST_CODE_RECEIVE_TRANSACTION)
                    }
                    else -> {
                    }
                }
            }
            val dlg = dialog.show()
            val wlp = dlg.window.attributes
            wlp.gravity = Gravity.BOTTOM
            dlg.window.attributes = wlp
        }

        btnExtract.setOnClickListener {
            if (UtilHelper.isOnline(context as Activity))
                if (!engine.isExtractPossible)
                    (activity as LoadedWalletActivity).toastHelper.showSingleToast(context, ctx.message)
                else if (ctx.card!!.remainingSignatures == 0)
                    (activity as LoadedWalletActivity).toastHelper.showSingleToast(context, getString(R.string.card_has_no_remaining_signature))
                else
                    (activity as LoadedWalletActivity).navigator.showPrepareTransaction(context as Activity, ctx)
            else
                Toast.makeText(activity, getString(R.string.no_connection), Toast.LENGTH_SHORT).show()
        }

        btnDetails.setOnClickListener {
            if (cardProtocol != null)
                (activity as LoadedWalletActivity).navigator.showVerifyCard(context as Activity, ctx)
            else
                (activity as LoadedWalletActivity).toastHelper.showSingleToast(context, getString(R.string.need_attach_card_again))
        }

        btnNewScan.setOnClickListener { (activity as LoadedWalletActivity).navigator.showMain(context as Activity) }

        // request card verify and get info listener
        val cardVerifyAndGetInfoListener: ServerApiTangem.CardVerifyAndGetInfoListener = object : ServerApiTangem.CardVerifyAndGetInfoListener {
            override fun onSuccess(cardVerifyAndGetArtworkResponse: CardVerifyAndGetInfo.Response?) {
                LOG.i(TAG, "cardVerifyAndGetInfoListener onSuccess")
                if (activity == null || !UtilHelper.isOnline(activity!!)) return

                val result = cardVerifyAndGetArtworkResponse?.results!![0]
                if (result.error != null) {
                    ctx.card!!.isOnlineVerified = false
                    return
                }
                ctx.card!!.isOnlineVerified = result.passed

                requestCounter--
                updateViews()

                if (!result.passed) return

                if (App.localStorage.checkBatchInfoChanged(ctx.card!!, result)) {
                    LOG.w(TAG, "Batch ${result.batch} info  changed to '$result'")
                    ivTangemCard.setImageBitmap(App.localStorage.getCardArtworkBitmap(ctx.card!!))
                    App.localStorage.applySubstitution(ctx.card!!)
                    refresh()
                }
                if (result.artwork != null && App.localStorage.checkNeedUpdateArtwork(result.artwork)) {
                    LOG.w(TAG, "Artwork '${result.artwork!!.id}' updated, need download")
                    requestCounter++
                    serverApiTangem.requestArtwork(result.artwork!!.id, result.artwork!!.getUpdateDate(), ctx.card!!)
                    updateViews()
                }
            }

            override fun onFail(message: String?) {
                LOG.i(TAG, "cardVerifyAndGetInfoListener onFail")
                if (activity == null || !UtilHelper.isOnline(activity!!)) return
                requestCounter--
                updateViews()
            }
        }
        serverApiTangem.setCardVerifyAndGetInfoListener(cardVerifyAndGetInfoListener)

        // request artwork listener
        val artworkListener: ServerApiTangem.ArtworkListener = object : ServerApiTangem.ArtworkListener {
            override fun onSuccess(artworkId: String?, inputStream: InputStream?, updateDate: Date?) {
                LOG.i(TAG, "artworkListener onSuccess")
                if (activity == null || !UtilHelper.isOnline(activity!!)) return
                App.localStorage.updateArtwork(artworkId!!, inputStream!!, updateDate!!)
                requestCounter--
                ivTangemCard.setImageBitmap(App.localStorage.getCardArtworkBitmap(ctx.card!!))
                updateViews()
            }

            override fun onFail(message: String?) {
                LOG.i(TAG, "artworkListener onFail")
                if (activity == null || !UtilHelper.isOnline(activity!!)) return
                requestCounter--
                updateViews()
            }
        }
        serverApiTangem.setArtworkListener(artworkListener)

        refresh()

        startVerify(lastTag)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(LoadedWalletViewModel::class.java)

        // set rate info to CoinData
        viewModel.getRateInfo().observe(this, Observer<Float> { rate ->
            ctx.coinData.rate = rate
            ctx.coinData.rateAlter = rate
        })
        viewModel.requestRateInfo(ctx)
    }

    override fun onPause() {
        super.onPause()
        if (timerHideErrorAndMessage != null) {
            timerHideErrorAndMessage!!.cancel()
            timerHideErrorAndMessage = null
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun onTransactionFinishWithSuccess(transactionFinishWithSuccess: TransactionFinishWithSuccess) {
        ctx.message = transactionFinishWithSuccess.message
        //ctx.coinData.clearInfo()
        updateViews()
        //srl?.isRefreshing = true
        srl?.postDelayed({ refresh() }, 10000)
    }

    @Subscribe
    fun onTransactionFinishWithError(transactionFinishWithError: TransactionFinishWithError) {
        ctx.error = transactionFinishWithError.message
        updateViews()
    }

    @Subscribe
    fun onDeleteWalletFinish(deletingWalletFinish: DeletingWalletFinish) {
        activity?.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constant.REQUEST_CODE_VERIFY_CARD ->
                // action after erase wallet
                if (resultCode == Activity.RESULT_OK)
                    activity?.finish()

            Constant.REQUEST_CODE_ENTER_NEW_PIN -> if (resultCode == Activity.RESULT_OK)
                if (data != null)
                    if (data.extras != null && data.extras!!.containsKey(Constant.EXTRA_CONFIRM_PIN))
                        (activity as LoadedWalletActivity).navigator.showPinRequestRequestPin(context as Activity, PinRequestActivity.Mode.RequestPIN.toString(), ctx, data.getStringExtra(Constant.EXTRA_NEW_PIN))
                    else
                        (activity as LoadedWalletActivity).navigator.showPinRequestConfirmNewPin(context as Activity, PinRequestActivity.Mode.ConfirmNewPIN.toString(), data.getStringExtra(Constant.EXTRA_NEW_PIN))


            Constant.REQUEST_CODE_ENTER_NEW_PIN2 -> if (resultCode == Activity.RESULT_OK)
                if (data != null)
                    if (data.extras != null && data.extras!!.containsKey(Constant.EXTRA_CONFIRM_PIN_2))
                        (activity as LoadedWalletActivity).navigator.showPinRequestRequestPin2(context as Activity, PinRequestActivity.Mode.RequestPIN2.toString(), ctx, data.getStringExtra(Constant.EXTRA_NEW_PIN_2))
                    else
                        (activity as LoadedWalletActivity).navigator.showPinRequestConfirmNewPin2(context as Activity, PinRequestActivity.Mode.ConfirmNewPIN2.toString(), data.getStringExtra(Constant.EXTRA_NEW_PIN_2))

            Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (newPIN == "")
                    newPIN = ctx.card!!.pin

                if (newPIN2 == "")
                    newPIN2 = App.pinStorage.piN2

                val pinSwapWarningDialog = PINSwapWarningDialog()
                pinSwapWarningDialog.setOnRefreshPage { (activity as LoadedWalletActivity).navigator.showPinSwap(context as Activity, newPIN, newPIN2) }
                val bundle = Bundle()
                if (!CardProtocol.isDefaultPIN(newPIN) || !CardProtocol.isDefaultPIN2(newPIN2))
                    bundle.putString(Constant.EXTRA_MESSAGE, getString(R.string.if_you_forget))
                else
                    bundle.putString(Constant.EXTRA_MESSAGE, getString(R.string.if_you_use_default))
                pinSwapWarningDialog.arguments = bundle
                activity?.supportFragmentManager?.let { pinSwapWarningDialog.show(it, PINSwapWarningDialog.TAG) }
            }

            Constant.REQUEST_CODE_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    ctx.saveToIntent(data)
                    data?.putExtra(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_DELETE)
                } else
                    data.putExtra(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_UPDATE)

                activity?.setResult(Activity.RESULT_OK, data)
                activity?.finish()

            } else {
                if (data != null && data.extras != null && data.extras!!.containsKey(EXTRA_TANGEM_CARD_UID) && data.extras!!.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getStringExtra(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundleExtra(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == Constant.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    (activity as LoadedWalletActivity).navigator.showPinRequestRequestPin2(context as Activity, PinRequestActivity.Mode.RequestPIN2.toString(), ctx)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey(Constant.EXTRA_MESSAGE)) {
                        ctx.error = data.getStringExtra(Constant.EXTRA_MESSAGE)
                    }
                }
            }

            Constant.REQUEST_CODE_PURGE -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    ctx.saveToIntent(data)
                    data?.putExtra(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_DELETE)
                } else
                    data.putExtra(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_UPDATE)

                activity?.setResult(Activity.RESULT_OK, data)
                activity?.finish()

            } else {
                if (data != null && data.extras != null && data.extras!!.containsKey(EXTRA_TANGEM_CARD_UID) && data.extras!!.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getStringExtra(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundleExtra(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == Constant.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++

                    val intent = Intent(activity, PinRequestActivity::class.java)
                    intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
                    ctx.saveToIntent(intent)
                    startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE)

                    return
                } else {
                    if (data != null && data.extras!!.containsKey(Constant.EXTRA_MESSAGE)) {
                        ctx.error = data.getStringExtra(Constant.EXTRA_MESSAGE)
                    }
                }
                updateViews()
            }

            Constant.REQUEST_CODE_SEND_TRANSACTION, Constant.REQUEST_CODE_RECEIVE_TRANSACTION -> {
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
                }
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        startVerify(tag)
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar?.post { rlProgressBar.visibility = View.VISIBLE }
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
                    if (!cardProtocol.card.isWalletPublicKeyValid)
                        refresh()
                    else
                        updateViews()

                    mpSecondScanSound.start()
                }
            } else {
                // remove last UIDs because of error and no card read
                rlProgressBar?.post {
                    lastReadSuccess = false
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported)
                        if (!NoExtendedLengthSupportDialog.allReadyShowed)
                            activity?.supportFragmentManager?.let { NoExtendedLengthSupportDialog().show(it, NoExtendedLengthSupportDialog.TAG) }
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
        WaitSecurityDelayDialog.onReadWait(activity as AppCompatActivity?, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(activity as AppCompatActivity?, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(activity)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

    }

    fun updateViews() {
        if (activity == null) return

        if (timerHideErrorAndMessage != null) {
            timerHideErrorAndMessage!!.cancel()
            timerHideErrorAndMessage = null
        }

        if (ctx.hasError()) {
            tvError?.visibility = View.VISIBLE
            tvError?.text = ctx.error
        } else {
            tvError?.visibility = View.GONE
            tvError?.text = ""
        }

        if (ctx.message == null || ctx.message.isEmpty()) {
            tvMessage.text = ""
            tvMessage.visibility = View.GONE
        } else {
            tvMessage.text = ctx.message
            tvMessage.visibility = View.VISIBLE
        }

        if (tvError.visibility == View.VISIBLE || tvMessage.visibility == View.VISIBLE) {
            timerHideErrorAndMessage = Timer()
            timerHideErrorAndMessage!!.schedule(
                    timerTask {
                        activity?.runOnUiThread {
                            tvMessage?.visibility = View.GONE
                            tvError?.visibility = View.GONE
                            // clear only already viewed messages
                            if (tvMessage.text == ctx.message) ctx.message = null
                            if (tvError.text == ctx.error) ctx.error = null
                        }
                    },
                    5000)
        }

        if (srl.isRefreshing) {
            tvBalanceLine1.setTextColor(resources.getColor(R.color.primary))
            tvBalanceLine1.text = getString(R.string.verifying_in_blockchain)
            tvBalanceLine2.text = ""
            tvBalance.text = ""
            tvBalanceEquivalent.text = ""
        } else {
            val validator = BalanceValidator()
            // TODO why attest=false?
            validator.check(ctx, false)
            context?.let { ContextCompat.getColor(it, validator.color) }?.let { tvBalanceLine1.setTextColor(it) }
            tvBalanceLine1.text = validator.firstLine
            tvBalanceLine2.text = validator.getSecondLine(false)
        }

        val engine = CoinEngineFactory.create(ctx)
        when {
            engine!!.hasBalanceInfo() -> {
                @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Html.fromHtml(engine.balanceHTML, Html.FROM_HTML_MODE_LEGACY)
                else
                    Html.fromHtml(engine.balanceHTML)
                tvBalance.text = html
                tvBalanceEquivalent.text = engine.balanceEquivalent
            }

            ctx.card?.offlineBalance != null -> {
                @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Html.fromHtml(engine.offlineBalanceHTML, Html.FROM_HTML_MODE_LEGACY)
                else
                    Html.fromHtml(engine.offlineBalanceHTML)
                tvBalance.text = html
            }

            else -> tvBalance.text = ""
        }

        tvWallet.text = ctx.coinData!!.wallet

        if (ctx.card!!.tokenSymbol.length > 1) {
            @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(ctx.blockchainName, Html.FROM_HTML_MODE_LEGACY)
            else
                Html.fromHtml(ctx.blockchainName)
            tvBlockchain.text = html
        } else
            tvBlockchain.text = ctx.blockchainName

        if (requestCounter == 0 && engine.hasBalanceInfo()) {
            btnExtract.isEnabled = true
            btnExtract.backgroundTintList = activeColor
        } else {
            btnExtract.isEnabled = false
            btnExtract.backgroundTintList = inactiveColor
        }

        if (engine.isNftToken) {
            btnLoad.isEnabled = false
            btnLoad.backgroundTintList = inactiveColor
            btnExtract.isEnabled = false
            btnExtract.backgroundTintList = inactiveColor
        }
    }

    private fun refresh() {
        if (ctx.card == null) return

        // clear all card data and request again
        ctx.coinData.clearInfo()
        ctx.error = null
        ctx.message = null

        LOG.w(TAG, "============= START REFRESH")
        requestCounter = 0
        srl?.isRefreshing = true

        updateViews()

        // Bitcoin, Litecoin, BitcoinCash
        if (ctx.blockchain == Blockchain.Bitcoin || ctx.blockchain == Blockchain.BitcoinTestNet || ctx.blockchain == Blockchain.Litecoin || ctx.blockchain == Blockchain.BitcoinCash) {
            ctx.coinData.setIsBalanceEqual(true)
        }

        requestVerifyAndGetInfo()

        requestBalanceAndUnspentTransactions()

        if (::viewModel.isInitialized)
            viewModel.requestRateInfo(ctx)

        if (requestCounter == 0) {
            // if no connection and no requests posted
            srl?.isRefreshing = false
            updateViews()
        }
    }

    private fun requestBalanceAndUnspentTransactions() {
        if (UtilHelper.isOnline(context as Activity)) {
            val coinEngine = CoinEngineFactory.create(ctx)
            requestCounter++
            coinEngine!!.requestBalanceAndUnspentTransactions(
                    object : CoinEngine.BlockchainRequestsCallbacks {


                        override fun onComplete(success: Boolean) {
                            LOG.i(TAG, "requestBalanceAndUnspentTransactions onComplete: $success, request counter $requestCounter")
                            if (activity == null) return
                            requestCounter--
                            if (!success) {
                                LOG.e(TAG, "requestBalanceAndUnspentTransactions ctx.error: " + ctx.error)
                            }
                            updateViews()
                        }

                        override fun onProgress() {
                            if (activity == null) return
                            LOG.i(TAG, "requestBalanceAndUnspentTransactions onProgress")
                            updateViews()
                        }

                        override fun allowAdvance(): Boolean {
                            return try {
                                context?.let { UtilHelper.isOnline(it) }!!
                            } catch (e: KotlinNullPointerException) {
                                e.printStackTrace()
                                false
                            }
                        }
                    }
            )
        } else {
            ctx.error = getString(R.string.no_connection)
            updateViews()
        }
    }

    private fun requestVerifyAndGetInfo() {
        if (UtilHelper.isOnline(context as Activity)) {
            if ((ctx.card!!.isOnlineVerified == null || !ctx.card!!.isOnlineVerified)) {
                LOG.i(TAG, "requestVerifyAndGetInfo")
                requestCounter++
                serverApiTangem.cardVerifyAndGetInfo(ctx.card)
            }
        } else {
            ctx.error = getString(R.string.no_connection)
            updateViews()
        }
    }

    private fun startVerify(tag: Tag?) {
        try {
            val isoDep = IsoDep.get(tag)
            val uid = tag?.id
            val sUID = Util.byteArrayToHexString(uid)
            if (ctx.card.uid != sUID || cardProtocol != null) {
                nfcManager.ignoreTag(isoDep.tag)
                return
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
            val txtShare = engine?.shareWalletUri.toString()
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = Constant.INTENT_TYPE_TEXT_PLAIN
            intent.putExtra(Intent.EXTRA_SUBJECT, Constant.WALLET_ADDRESS)
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

}