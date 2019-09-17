package com.tangem.ui.fragment.wallet

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
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tangem.App
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.data.dp.PrefsManager
import com.tangem.data.network.ServerApiCommon
import com.tangem.server_android.ServerApiTangem
import com.tangem.server_android.model.CardVerifyAndGetInfo
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.reader.CardProtocol
import com.tangem.tangem_card.tasks.VerifyCardTask
import com.tangem.tangem_card.util.Util
import com.tangem.tangem_sdk.android.reader.NfcReader
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD_UID
import com.tangem.tangem_sdk.data.loadFromBundle
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.PINSwapWarningDialog
import com.tangem.ui.dialog.ShowQRCodeDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.ui.event.DeletingWalletFinish
import com.tangem.ui.event.TransactionFinishWithSuccess
import com.tangem.ui.fragment.BaseFragment
import com.tangem.ui.fragment.pin.PinRequestFragment
import com.tangem.ui.fragment.pin.PinSwapFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.util.LOG
import com.tangem.util.UtilHelper
import com.tangem.wallet.*
import kotlinx.android.synthetic.main.fr_loaded_wallet.*
import kotlinx.android.synthetic.main.layout_btn_details.*
import kotlinx.android.synthetic.main.layout_tangem_card.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.InputStream
import java.util.*
import kotlin.concurrent.timerTask

class LoadedWalletFragment : BaseFragment(), NavigationResultListener, NfcAdapter.ReaderCallback,
        CardProtocol.Notifications, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        val TAG: String = LoadedWalletFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fr_loaded_wallet

    private lateinit var viewModel: LoadedWalletViewModel
    private lateinit var ctx: TangemContext
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
    private var refreshAction: Runnable? = null
    private val inactiveColor: ColorStateList by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            resources.getColorStateList(R.color.btn_dark, activity?.theme)
        else
            @Suppress("DEPRECATION")
            resources.getColorStateList(R.color.btn_dark)
    }
    private val activeColor: ColorStateList by lazy {
        val color = if( (Util.bytesToHex(ctx.card?.cid)?.startsWith("10") == true)) {
           R.color.start2coin_orange
        } else {
            R.color.colorAccent
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            resources.getColorStateList(color, activity?.theme)
        else
            @Suppress("DEPRECATION")
            resources.getColorStateList(color)
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
        ctx = TangemContext.loadFromBundle(context, arguments)

        lastTag = activity?.intent?.getParcelableExtra(Constant.EXTRA_LAST_DISCOVERED_TAG)

        if (arguments?.containsKey(NfcAdapter.EXTRA_TAG) == true) {
            val tag = arguments!!.getParcelable<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) onTagDiscovered(tag)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mpSecondScanSound = MediaPlayer.create(activity, R.raw.scan_card_sound)

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

        if (Util.bytesToHex(ctx.card?.cid)?.startsWith("10") == true) {
            btnLoad?.visibility = View.GONE
        }

        btnLoad.setOnClickListener {
            val items = arrayOf<CharSequence>(getString(R.string.loaded_wallet_load_via_app), getString(R.string.loaded_wallet_load_via_share_address), getString(R.string.loaded_wallet_load_via_qr))//, getString(R.string.via_cryptonit), getString(R.string.via_kraken))
            val cw = android.view.ContextThemeWrapper(activity, R.style.AlertDialogTheme)
            val dialog = AlertDialog.Builder(cw).setItems(items
            ) { _, which ->
                when (items[which]) {
                    getString(R.string.loaded_wallet_load_via_app) -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, engine.shareWalletUri)
                            intent.addCategory(Intent.CATEGORY_DEFAULT)
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            (activity as MainActivity).toastHelper.showSingleToast(context, getString(R.string.loaded_wallet_no_compatible_wallet))
                        }
                    }

                    getString(R.string.loaded_wallet_load_via_share_address) -> {
                        doShareWallet(true)
                    }

                    getString(R.string.loaded_wallet_load_via_qr) -> {
                        ShowQRCodeDialog.show(activity as AppCompatActivity?, engine.shareWalletUri.toString())
                    }

                    getString(R.string.loaded_wallet_load_via_cryptonit) -> {
                        navigateForResult(
                                Constant.REQUEST_CODE_RECEIVE_TRANSACTION,
                                R.id.action_loadedWalletFragment_to_prepareCryptonitWithdrawalFragment,
                                Bundle().apply { ctx.saveToBundle(this) }
                        )
                    }

                    getString(R.string.loaded_wallet_load_via_kraken) -> {
                        navigateForResult(
                                Constant.REQUEST_CODE_RECEIVE_TRANSACTION,
                                R.id.action_loadedWalletFragment_to_prepareKrakenWithdrawalFragment,
                                Bundle().apply { ctx.saveToBundle(this) }
                        )
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
                if (!engine.isExtractPossible) {
                    (activity as MainActivity).toastHelper.showSingleToast(context, ctx.message)
                } else if (ctx.card!!.remainingSignatures == 0) {
                    (activity as MainActivity).toastHelper.showSingleToast(context, getString(R.string.loaded_wallet_warning_no_signature))
                } else {
                    val bundle = Bundle().apply { ctx.saveToBundle(this) }
                    navigateForResult(Constant.REQUEST_CODE_SEND_TRANSACTION,
                            R.id.action_loadedWalletFragment_to_prepareTransactionFragment, bundle)
                }
            else
                Toast.makeText(activity, getString(R.string.general_error_no_connection), Toast.LENGTH_SHORT).show()
        }

        btnDetails.setOnClickListener {
            if (cardProtocol != null) {
                val bundle = Bundle().apply { ctx.saveToBundle(this) }
                navigateForResult(Constant.REQUEST_CODE_VERIFY_CARD,
                        R.id.action_loadedWalletFragment_to_verifyCard, bundle)
            } else {
                (activity as MainActivity).toastHelper
                        .showSingleToast(context, getString(R.string.general_notification_scan_again_to_verify))
            }
        }

        btnNewScan.setOnClickListener {
            //            navigateToDestination(R.id.action_loadedWalletFragment_to_main)
            navigateUp()
        }

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

    override fun onStop() {
        srl?.removeCallbacks(refreshAction)
        super.onStop()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun onTransactionFinishWithSuccess(transactionFinishWithSuccess: TransactionFinishWithSuccess) {
        refreshAction = Runnable { refresh() }
        srl?.postDelayed(refreshAction, 10000)
    }

    @Subscribe
    fun onDeleteWalletFinish(deletingWalletFinish: DeletingWalletFinish) {
        navigateUp()
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        when (requestCode) {
            Constant.REQUEST_CODE_VERIFY_CARD ->
                // action after erase wallet
                if (resultCode == Activity.RESULT_OK)
                    navigateUp()

            Constant.REQUEST_CODE_ENTER_NEW_PIN -> if (resultCode == Activity.RESULT_OK)
                if (data != null)
                    if (data.containsKey(Constant.EXTRA_CONFIRM_PIN)) {
                        val bundle = PinRequestFragment.callingIntentRequestPin(
                                PinRequestFragment.Mode.RequestPIN.toString(), ctx,
                                data.getString(Constant.EXTRA_NEW_PIN) ?: "")

                        navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN,
                                R.id.action_loadedWalletFragment_to_pinRequestFragment,
                                bundle)
                    } else {
                        val bundle = PinRequestFragment.callingIntentConfirmPin(
                                PinRequestFragment.Mode.ConfirmNewPIN.toString(),
                                data.getString(Constant.EXTRA_NEW_PIN) ?: "")

                        navigateForResult(Constant.REQUEST_CODE_ENTER_NEW_PIN,
                                R.id.action_loadedWalletFragment_to_pinRequestFragment, bundle)
                    }


            Constant.REQUEST_CODE_ENTER_NEW_PIN2 -> if (resultCode == Activity.RESULT_OK)
                if (data != null)
                    if (data.containsKey(Constant.EXTRA_CONFIRM_PIN_2)) {
                        val bundle = PinRequestFragment.callingIntentRequestPin2(
                                PinRequestFragment.Mode.RequestPIN2.toString(), ctx,
                                data.getString(Constant.EXTRA_NEW_PIN_2))
                        navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN,
                                R.id.action_loadedWalletFragment_to_pinRequestFragment, bundle)

                    } else {
                        val bundle = PinRequestFragment.callingIntentConfirmPin2(
                                PinRequestFragment.Mode.ConfirmNewPIN2.toString(),
                                data.getString(Constant.EXTRA_NEW_PIN_2))
                        navigateForResult(Constant.REQUEST_CODE_ENTER_NEW_PIN2,
                                R.id.action_loadedWalletFragment_to_pinRequestFragment, bundle)
                    }

            Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (newPIN == "")
                    newPIN = ctx.card!!.pin

                if (newPIN2 == "")
                    newPIN2 = App.pinStorage.piN2

                val pinSwapWarningDialog = PINSwapWarningDialog()
                pinSwapWarningDialog.setOnRefreshPage {
                    val bundle = PinSwapFragment.callingIntent(newPIN, newPIN2)
                    navigateForResult(Constant.REQUEST_CODE_SWAP_PIN, R.id.pinSwapFragment, bundle)
                }
                val bundle = Bundle()
                if (!CardProtocol.isDefaultPIN(newPIN) || !CardProtocol.isDefaultPIN2(newPIN2))
                    bundle.putString(Constant.EXTRA_MESSAGE, getString(R.string.loaded_wallet_warning_dont_forget_pin))
                else
                    bundle.putString(Constant.EXTRA_MESSAGE, getString(R.string.loaded_wallet_warning_default_pin))
                pinSwapWarningDialog.arguments = bundle
                activity?.supportFragmentManager?.let { pinSwapWarningDialog.show(it, PINSwapWarningDialog.TAG) }
            }

            Constant.REQUEST_CODE_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    val resultData = Bundle()
                    ctx.saveToBundle(resultData)
                    resultData.putString(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_DELETE)
                    navigateBackWithResult(Activity.RESULT_OK, resultData)
                } else {
                    data.putString(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_UPDATE)
                    navigateBackWithResult(Activity.RESULT_OK, data)
                }

            } else {
                if (data != null && data.containsKey(EXTRA_TANGEM_CARD_UID) && data.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getString(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundle(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == Constant.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val bundle = PinRequestFragment.callingIntentRequestPin2(
                            PinRequestFragment.Mode.RequestPIN2.toString(), ctx)
                    navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN,
                            R.id.action_loadedWalletFragment_to_pinRequestFragment, bundle)
                    return
                } else {
                    if (data != null && data.containsKey(Constant.EXTRA_MESSAGE)) {
                        ctx.error = data.getString(Constant.EXTRA_MESSAGE)
                    }
                }
            }

            Constant.REQUEST_CODE_PURGE -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    val resultData = Bundle()
                    ctx.saveToBundle(resultData)
                    resultData.putString(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_DELETE)
                    navigateBackWithResult(Activity.RESULT_OK, resultData)
                } else {
                    data.putString(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_UPDATE)
                    navigateBackWithResult(Activity.RESULT_OK, data)
                }

            } else {
                if (data != null && data.containsKey(EXTRA_TANGEM_CARD_UID) && data.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getString(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundle(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == Constant.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++

                    val bundle = Bundle()
                    bundle.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
                    ctx.saveToBundle(bundle)
                    navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE,
                            R.id.action_loadedWalletFragment_to_pinRequestFragment, bundle)

                    return
                } else {
                    if (data != null && data.containsKey(Constant.EXTRA_MESSAGE)) {
                        ctx.error = data.getString(Constant.EXTRA_MESSAGE)
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

                if (data != null) {
                    if (data.containsKey(EXTRA_TANGEM_CARD_UID) && data.containsKey(EXTRA_TANGEM_CARD)) {
                        val updatedCard = TangemCard(data.getString(EXTRA_TANGEM_CARD_UID))
                        updatedCard.loadFromBundle(data.getBundle(EXTRA_TANGEM_CARD))
                        ctx.card = updatedCard
                    }
                }
            }
            else -> if (data != null && data.containsKey(Constant.EXTRA_MESSAGE)) {
                when (resultCode) {
                    Activity.RESULT_CANCELED -> ctx.error = data.getString(Constant.EXTRA_MESSAGE)
                    Activity.RESULT_OK -> ctx.message = data.getString(Constant.EXTRA_MESSAGE)
                }
                updateViews()
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        startVerify(tag)
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar?.post { rlProgressBar?.visibility = View.VISIBLE }
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
                            Toast.makeText(activity, R.string.general_notification_scan_again, Toast.LENGTH_SHORT).show()
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
            tvMessage?.text = ""
            tvMessage?.visibility = View.GONE
        } else {
            tvMessage?.text = ctx.message
            tvMessage?.visibility = View.VISIBLE
        }

        if (tvError?.visibility == View.VISIBLE || tvMessage?.visibility == View.VISIBLE) {
            timerHideErrorAndMessage = Timer()
            timerHideErrorAndMessage?.schedule(
                    timerTask {
                        activity?.runOnUiThread {
                            tvMessage?.visibility = View.GONE
                            tvError?.visibility = View.GONE
                            // clear only already viewed messages
                            if (tvMessage?.text == ctx.message) ctx.message = null
                            if (tvError?.text == ctx.error) ctx.error = null
                        }
                    },
                    5000)
        }

        if (srl.isRefreshing) {
            tvBalanceLine1.setTextColor(resources.getColor(R.color.primary))
            tvBalanceLine1.text = getString(R.string.loaded_wallet_verifying_in_blockchain)
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

        // Bitcoin, Litecoin, BitcoinCash, Stellar
        if (ctx.blockchain == Blockchain.Bitcoin || ctx.blockchain == Blockchain.BitcoinTestNet ||
                ctx.blockchain == Blockchain.Litecoin || ctx.blockchain == Blockchain.BitcoinCash ||
                ctx.blockchain == Blockchain.Stellar || ctx.blockchain == Blockchain.StellarTestNet) {
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

                            if (coinEngine.isBalanceNotZero) showWarningIfPendingTransactionIsPossible()
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
            ctx.error = getString(R.string.general_error_no_connection)
            updateViews()
        }
    }

    private fun showWarningIfPendingTransactionIsPossible() {
        if (ctx.card.signedHashes > 0 && isNewCid(ctx.card.cidDescription)) {
            AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_warning)
                    .setMessage(R.string.loaded_wallet_warning_card_signed_transactions)
                    .setPositiveButton(R.string.general_ok) { _, _ -> }
                    .create()
                    .show()
        }
        PrefsManager.getInstance().appendCid(ctx.card.cidDescription)
    }

    private fun isNewCid(cid: String): Boolean = !PrefsManager.getInstance().getAllCids().contains(cid)

    private fun requestVerifyAndGetInfo() {
        if (UtilHelper.isOnline(context as Activity)) {
            if ((ctx.card!!.isOnlineVerified == null || !ctx.card!!.isOnlineVerified)) {
                LOG.i(TAG, "requestVerifyAndGetInfo")
                requestCounter++
                serverApiTangem.cardVerifyAndGetInfo(ctx.card)
            }
        } else {
            ctx.error = getString(R.string.general_error_no_connection)
            updateViews()
        }
    }

    private fun startVerify(tag: Tag?) {
        try {
            val isoDep = IsoDep.get(tag)
            val uid = tag?.id
            val sUID = Util.byteArrayToHexString(uid)
            if (ctx.card.uid != sUID || cardProtocol != null) {
                (activity as MainActivity).nfcManager.ignoreTag(isoDep.tag)
                return
            }

            if (lastReadSuccess)
                isoDep.timeout = 1000
            else
                isoDep.timeout = 65000

            verifyCardTask = VerifyCardTask(ctx.card, NfcReader((activity as MainActivity).nfcManager, isoDep),
                    App.localStorage, App.pinStorage, App.firmwaresStorage, this)
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
                val chooser = Intent.createChooser(intent, getString(R.string.loaded_wallet_chooser_share))

                // verify the intent will resolve to at least one activity
                if (intent.resolveActivity(activity!!.packageManager) != null) {
                    startActivity(chooser)
                }
            } else {
                val clipboard = activity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip = ClipData.newPlainText(txtShare, txtShare)
                Toast.makeText(activity, R.string.loaded_wallet_toast_copied, Toast.LENGTH_LONG).show()
            }
        } else {
            val txtShare = ctx.coinData.wallet
            val clipboard = activity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = ClipData.newPlainText(txtShare, txtShare)
            Toast.makeText(activity, R.string.loaded_wallet_toast_copied, Toast.LENGTH_LONG).show()
        }
    }

}