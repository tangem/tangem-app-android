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
import com.tangem.tangem_card.tasks.WriteIDCardTask
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
import java.text.SimpleDateFormat
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
        val color = if ((Util.bytesToHex(ctx.card?.cid)?.startsWith("10") == true)) {
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
            requestCounter--
            updateViews()
        })
        requestCounter++
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

                    // TODO - remove, only for test&demo reason!
                    // TODO - begin
                    if (cardProtocol.card.isIDCard) {
                        if (cardProtocol.card.hasIDCardData()) {
                            Toast.makeText(activity, "ID card: ${cardProtocol.card.idCardData.fullName}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, "Empty ID card", Toast.LENGTH_SHORT).show()
                        }
                    }
                    // TODO - end

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
        if (activity == null || this.view == null) return

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
            context?.let { ContextCompat.getColor(it, validator.color) }?.let { tvBalanceLine1?.setTextColor(it) }
            tvBalanceLine1?.text = getString(validator.firstLine)
            tvBalanceLine2?.text = getString(validator.getSecondLine(false))
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
        if (ctx.blockchain == Blockchain.Bitcoin || ctx.blockchain == Blockchain.BitcoinTestNet || ctx.blockchain == Blockchain.BitcoinDual ||
                ctx.blockchain == Blockchain.Litecoin || ctx.blockchain == Blockchain.BitcoinCash ||
                ctx.blockchain == Blockchain.Stellar || ctx.blockchain == Blockchain.StellarTestNet || ctx.blockchain == Blockchain.StellarAsset) {
            ctx.coinData.setIsBalanceEqual(true)
        }

        requestVerifyAndGetInfo()

        requestBalanceAndUnspentTransactions()

        if (::viewModel.isInitialized) {
            requestCounter++
            viewModel.requestRateInfo(ctx)
        }

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
        if (ctx.card.signedHashes > 0 && isNewCid(ctx.card.cidDescription)
                && ctx.card.signedHashes != ctx.coinData.sentTransactionsCount) {
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

                //TODO - remove, only for test
                //TODO - begin
                if (ctx.card.isIDCard && !ctx.card.hasIDCardData()) {
                    val idCardData = TangemCard.IDCardData()
                    idCardData.fullName = "John Doe"
                    idCardData.birthday = "1980-1-1"
                    idCardData.gender = "M"
                    val dt = Date()
                    idCardData.issueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dt)
                    dt.year = dt.year + 10
                    idCardData.expireDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dt)
                    idCardData.trustedAddress = "GCOQLQT32RVH2MTWQGK5K3GIGT3STYG5323KMNTTFHIU2T5TFOGR4KDT" // ask D.Baturin
                    idCardData.photo = Util.hexToBytes("FFD8FFE000104A46494600010101012C012C0000FFDB0043000A07070807060A0808080B0A0A0B0E18100E0D0D0E1D15161118231F2524221F2221262B372F26293429212230413134393B3E3E3E252E4449433C48373D3E3BFFDB0043010A0B0B0E0D0E1C10101C3B2822283B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3B3BFFC000110800C8007903012200021101031101FFC4001C0000020203010100000000000000000000000706080103050402FFC4003F100001030301050505060406020300000001020304000511060712213141135161718114223291A115235262B1C1428292D10816333472A2175393D2E1FFC4001801000301010000000000000000000000000002030104FFC4002111000202030100020301000000000000000001021103213112132232415142FFDA000C03010002110311003F007351451400562B35A83ED29F53097505D424294807DE4839C123B8E0FCA8016BB4CD712AC97FB642B72FDF8AA12A4273C179C84A0F811BC7D45306CF768B7BB547B9425EFB0FA3793DE3BC1F10720D579D75297335C5E1D5924A64A9B19EE4FBA3F4AECECDB5B7F96AE2604E708B64B57BC4F2617C82FC8F23E87A54164FB6C771D0FBA2BE50B4AD0149214923208E44566AE219A28A2800A28A2800A28A2800A28A2800A28A2803E5440049E02ABEFF009F26C5DA0C9D44CA94B69C74B6A673C16C03809F900478D580791DAB2B6F38DF494E7CEAA9BAD165E5B4AF89B5149F307151CADAAA1A28ECEB35B0FEAB9D322381C8D31424B4B1D52B483F30720F8835C4A3248009381CBC28AE76ED9444C7496D2AEDA65B44375227C04F04B2E2B0A6C7E45741E07879533AD7B55D2B7040ED662A0B879A2520A47F50C8FAD2028A78E468571459E6F53D85E46FB77AB7A93DE24A3FBD706EBB54D2D6DCA5B98A9CE0FE18A8DE1FD4709FAD57EC03D07CAB34CF3333C967B4E5F19D47638D7561B53687C1FBB5104A48241071E55EE932E3C3614FCA7DB61A4FC4E38A0948F5355F6C1B43BBE9BB02ED36F6D8CA9E538979D05451903202797319E3DFCAB8574BD5CEF6FF006D739CF4B5E78768AC84F90E43D053FCAA8CF2588B36AFB46A0B9C8856A7952BD99016E3C9410D8C9C0009E679F2E1C2BBB4BED8EDA7D8B4A393D69C393DE2A07F227DD4FD778FAD306A916DAB6630A28A298C31466B54A694FC575943CB654B414A5C47C4824731E2290978D5FAE2C976936C977B901E8EB2927711850E8A1EEF22307D69252F26A564CEFF00B49BCE92D48FDB6E36D8F2E383DA30EB6A2D296D9E5DE091C41F2A52DD65353AEF32630D29A6A43EB752DA8E4A428938FAD6FBBDFEEB7E5B4BBACD5CA532084296948290798E0077573AB9A53B2895051582A006490078D7C7B4320E3B44FCE9526CD3651520B268ABA6A0483024DB5448CEE19C82BF54A72457A6E9B37D51698AECA760B6FB2CA0AD6A61E4A8800649C7034530B22D45789570E3EEB7F335F22E0BEADA4F91A7F8A467A47BE83E15E76A634E1C1F70F8D7A291C5AE9A9D8D7B0ED76D36BB544B6AACB2D0DC6692D02D3885E70319C1C73A6258AFE9BEB1DBB76DB8446F190A96C86F7BCB8E4D56C813E55B26B53613CA62432ADE42D38C83EB4E2D15B556EF1218B5DE582CCD748436F3292A43A7C40E293F4F2ABC277A62490C8A2B159AB8862951B68B75B0B712E224B2D5C53F765927DF79BEFC7E53D4F426999738D2E5C25330E7182EAB876C9682D491E00F0CF8F1A40EBEB55AECB75EC5ABFBD75B82944CB5BC527B33D0139E2AF0E953C9C1A3D22F5A24C8EC123032A3CBBAB7020F2391E15BA2C11739B1E0938F68790D820711BCA033F5AE68D5EC77C3970E15C2F32D31E1467A5BEAE4DB482A3F21D2A52C6C8F5A3CD05FD9496F3C92E486D27E59A7BE9ED3569D236AF6580D25A42465E7DC237DC3F894AFDB90AC2F58E986DEEC57A86D895E71BA6523FBD59E47FE5095FD10917671AA23DFE0439B6A931DB7E4210A7D1EF21033C4EF24903866AC9169B2D762520B653BBBA78E538C63E55861F664B21E8EF21D695C96DA82927D452FB5D6D43EC49E6C7608E275D490859DD2A4B4A3FC21238A95E1C878F2A46DCDD1BA479E06C32C2D2D6B9B3A649CA894A1B21B4A46780EA4FCC56ABEEC36D521ADFB24C7A23C07FA7215DA215EB8C8FAD781AD21B56D449126E17E55B82F88695254D91FC8D8C0A1CD27B58D3A3B7817D55C528E3D9A649709FE57471F4AAF99FF004CB42E351E8FBDE977B72E509686C9C21E4FBCDABC94387EF5E78A9712C80E1C9E80F414D381B4C45E62CBD2FACA10B7497DB5305F282942564606FA4F141CE0E797952CD68536B5217F12490AF31CEA7372AA66C523E69F7B33B169F896166E76B57B5497D387A4B806FA55D518FE1C1E9D79E4D2523E9BD473612AE10EC52DF86919ED529F8C0E6523991E40D31F64F11C5155CAC3770EC65283770B74B6F716D9E8A041209EE381919071D371C5A7612636EB358159AE810876D3F53BDA6748BAEC3514CD96B11E3A8734920E543C40071E24579F44681B6E9EB434E4B88D49BA3C90B92FBC80B2147894A73C80E5E278D7236C984CCD2AE3BFEDD370FBDCF2E68E7E80D320F33E751CADF068A10FB53B4C5B56AF262212DA2530878B6848012AC949C01DFBB9F9D462D17362CD798572928538DC5792E96D3CD653C401EB8A97ED817BDACDB4FE086D8FAA8D426CD6A3A8355DBACFBC52890F250B23984F3511E80D4A11F5219BA44D615A358ED764AA6CE99EC1670B3B8307B3183C908CFBE47551F9F4AF5DE767DB39D33B912F5A9673531C00E13BAA207794A50703CE9CD162B16F84DC68ACA5A61840436DA0704A40E005548BD5CA45DEF32EE129654F48754B513D38F01E4070F4AEBAA264F275B6FDB305C7BEE9DBBA6E165998DD71232DAB23805A738F250EA3A72AEF6C36CAD4D72E5A9660EDE507BB1696BE25248DE5ABCCEF019F3EFAD5616C9FF0F574F6FE2CEF3AA8FBDD3DF4EEE3F9F35AF613A8988F26769F7D610B92A1223E7F89406149F3C007D0D0074F52EDBBEC8D46F5BADF6B6E5468AE169D756E9495A81C2B7703000391939CD31B4E5FE16A7B247BAC1512D3C38A55F121438149F106AB56BBB1CAB06AFB846908210E3CA759591C16DA89208EFE783E20D3736131E533A3E53AF052587A6294CE7AE1290A23C3231E8680241AFB41C1D636A5E1B437736924C6918C1CFE051EA93F4E755CD80F34F3B0DE4143CD28A0A55CC107047A1AB7279555FD6FD89DA4DE7D9F1B9ED4BCE3F163DEFF00B66926AE2CD5D2C6DAA208369870C0C7B3B086FE490297D15B1A676EC23C61D9C5BEC52B5B63E10BC28938FF009209FE6358D9AEBF125B458AF2F80F21388D21C57FA807F0289EA0723D478F3F88529BD5FB6F6675BD41E83648A52A7D1C50A5614381EBEF2CE3BF74D47174690D8159AC0ACD748847B5B694635869D76D8EB9D93A141C61DC6771C1C8F910483E06A0D17556BDD22C2605F74BBD7669801089914925491C8920107D403DF4DAAC1AC693E8595A3575FD7A9750BD725C572265296C30E1CA91BA3183E39CD78ECD25FD35A82D1A95D654A862491BC3AEEE02C79EEAABEF51C77226A3B8B0F496E53A990BED1D6F3BAA51393CFC4E3D29A7A2B4BDBB516CB99B7DC992B69F7DD752A4F0536ADE202927A1E15CD19799146AD0C5833A2DCA135321BE87E3BC90A6DC41C850A5DDD7629619B7B76E7EDF222C57165D7632027033C4E147E11E87151FFF00C6DAEF4BBEBFF2B5F7B48CA39DD4BDD913FF00242BDD27C73515D6E35D4211A2EA6B9BCEFB58514474C90A07040C94A78733C2BA1493E13A247AF351C7BF983A0B46321D8ADAD285167E070A79241EA91CCABA919E993D0D47B227A1DAA04ED32E94DD203490E84AB74BEB1C77D27A2B3D3A8C75E732D13A12D9A420A16D35DA5C1D6C07E439C559C64A53DC9CFCFAD4AAA32C8EF43A889DB6ED3AD5706D169DA1D8D0B9118E0BEE460BC1EF5208CA4F7E39F753021EBED12DC34262DF6DECB084E10D85767BA3B827031F2AF6DE74C593502026ED6D625103016A4E163C943047CE92BB50D176BD2371B649B7B0E88127783AD29D2AC292412013C46527E9548E452D18D5135D5BB67B6468AB89A68AA7CD706EA1EECC86DB27A8CF151EE0063F4A80CDD0D3ECFA3D3A8AEFBE89F2E5A53D8B87DE4A141472AFCC4E0E3A0F3A72E9DD17A5ECA96A65A6D8D071690B4487097178232082ACE3D315AF5E69995AAACCCC18D25A8FD9BE1E5A9C04E40491818F3A9CF25E91A908BB346B5CC9E98F769CEC261CE01F4341C4A4FE6191C3C45582D17A6E1699B0B70E1A98782CEFAA4328DDEDF3C94789CF0F1C776290B61F6190B7ED53FB3684C012CCB50FF6EF0CEE927F01CEEABCC1E94E6D9DE96BE69A82A6AE972438CAC6510D037C327A90B3FA018ADC46489AD158ACD5C50A8D6BBD4A9D33A69F94850F6B7BEE62A7BD67AFA0C9F4A92D41B6A3A818B1D89296D0DAAE52829A8CB29054D24FC6B07A70C0F323BA964E91A8439254A2A512493924F326AC2ECF59EC7425A138C6F31BFF00D4A27F7AAF237411BD9DDEB8EEAB31A762A6169CB6C54AD2B0D456D2149E4AF747115C654E8D26F5AA7EDADB7D9ADABE2DB1D82549F0C9715F4A7252727FDCFF0088C8CB74E12B537BB9F16303EB54C7D62C871F3E3DF59AC0E559A98C14BADB6C44BFA25A7F1EF4798820F8282927F6A62D2EB6D935B8FA25B8C4FBF265A02478241513FA7CE9E1F9215F092E8394A99A12CAF2CE55EC88493DFBBEEFED5DD75412DA94790493F4AE0E828AB85A12CCCB80857B225641E9BDEF7EF5EBD513D36BD2F729AA38ECA32F77FE44607D48A57D3570AD8A3BCA51E849A766CAB5A7DAF6FF00B127BB99B111F74A51E2F343F529E47C307BE92206001DD5E9B7CF936AB8313E1BA5A911D616DA8743FD8F235B097960D596A6B35C4D29A9236A8B1B370630959F75E6B3C5A58E63F71E04576EBAD3B247CA88032780155B3596A273536A491394486527B28E83FC2D83C3D4F127CE9F7AC2E3F65692BA4D070A6E32820FE623753F522AB3018E1DD51CCFF43C4CD3E76617B4DDB4830C2D797E01F677013C7747141FE9E1E8690D5DDD21AAA4E93BB898D23B561C1B9219CE37D3E1DC474FFF006B9C72C6528B6C76D936BBCDA757C2202D85A1A59CF25A49520F8E4647A577646D92C0DC7DE8F1273CEE3820A12819F1564FE94B2D57AB2E1ABA6076684B6C3790CC647C280799F127BE9E32A7663563CB4BEAEB5EACB737260BE80F1482EC62A1DA34AEA08EEEE3C8D77307B8FCAAA608AA69C0E47794D287220E08F222BD2A9B7B5A77177896A477190B23E59A6A8BE3336593BDEA8B2E9D8EA76E97165820706F7B79C578048E26945DA4FDB16B76886571EC9031BC09E286C9C9C9FC6BC63872C7852FC420A5153AE2964F3F1F5A9468BD52EE8FBA990D33DAC6753B8FB20E37873041EF1FDE8B8C79D0A6CB148425B42508484A523000E400E94ABDAF6A76D486F4EC570294141D9641F871F0A3CFA9F4AF8BFED8FB786A62C509D61D58C191237728FF008A46727C4FCA960E38E3CEADD756A71C5A8A94B51C9513CC935318F9A28A2B00636CD6DFAAECD3A3DCE2DB1C91699C901EDD751EF273C16015734FE9914EAE3E1503D8F5C7DAF46988A56550A42DBC7E557BC3F53F2A9ED76415224FA2F76CD70F66D28C4249C2A6494823BD281BC7EBBB490A636DA6E3DBEA285012ACA62C6DF50EE52CFF00648A5CD73E4772291E05145153342B5B921B68E16AC1EEC56CAF9536851CA90924778AD55FB035A65B0A380B03CC62B766B52E3B2B182D8F4E15AA3EF32FA982494E32934F49AD196CF551451533428A28A0028A283C050032B62971EC6F770B7295812180EA47E641C1FA2BE94E7CD577D1E6569BD79685CB41692FA90013F0ADB75380A07A8E23E5561BDEEEAEAC4FEB44E5D14ACE8E7B5DEB6B85F26A94DD9848DC694382A4A5184809EE4F0E2AF9782BA404894E840C2438A091DC327156A5C09663AB7404A508380060000555351DE515779CD4F2248D8B31451454470A28A2800A31451400514514005145140051451400E6D2F6585ADB6690187CF6732DEA5B6C4948CA995A5594F98C6EE454DF7F50FF00E9B77FF22FFF00AD2F762170CB374B6A95F0A90FA0798293FA0A6BD76436AC93E9E4B9AFB3B5CB5FE16167FEA6AAEC3892273A962334A75D282ADD4F3C049513E8013566F512FB3D37735E71BB0DD3FF0043506D9368F4C1B3AAF539A1ED13DBDD692A1F0327F7573F2C52CE3E9A46A7484C03919A2BD57582AB65DA64058C18CFADAF92881F4C5796B95940A28A2800A28A2800A28A2800A28A2803D56CB6C8BBDC1B811021521EC86D2B504EF9033BA09E1938E15F1360CBB74A5459B19D8CFA3E26DD4949AF88D25E8729A951D5B8F32B0E36A1D140E47E95635B8D65D75A6A24B9B09A92CC9682D3BC3DE6D5D40573041C8E1DD548414918DD0A2D935C3D8B5CB0D156133195B27CF1BC3EA9FAD3F37877D2AA4ECAE6586FB0EF1A7A47B5371A421D319E2038003C40572570CF3C7AD357753DD57C69A54C9B354B8ACCE88EC5909DF65E414389CFC493C08AD8942508094A425206001C80AFBAC550C2BFED56DDEC1AEA4B813BA898DA1F1E646E9FAA6A1D4DADB75BB2D5AEE694FC2A5C759F31BC9FD15503D21A4A66ADBA88AC65B8CDE15224638369F0EF51E83F6AE49C7ED48A27A38C224930CCC0C39ECC970345EDD3BA16464273DF8AD5565D5A4ED074C2B4EA6304C12DEE6E8F8B3F8F3F8B3C73DF55EF50D865E9BBD3F6C9832A6CE50B0301C41E4A1E7F439144F1F904ECE6515BE14291719CCC288D175F7D610DA075269FB6ED9D5958D22DD866C74C827EF1D7C7059748E2A49E631C8780AC841C8D6E8AF4738E0327A0A91EAAD1772D2A63BB2125D8B21092979238256464A15DC471C778AEF3BB31B95A758DB23A92A976C7E52712529F8520EF10B1D0E0791FA53A2E16F897582EC29CC25F8EF2775685F223F63E34F1C769D8AE4557A9AECE34546D5AEDC173CBA88CC361085B6AC10E2B883E3800F0F115E4D73A1A56919BDA2379FB6BCAFB97C8E293F817DC7C7AD37B67164FB1346C36DC4EEBF247B43BDF957103D13814421F6A66B7A14FAAB66B7AD39BF21A419F053C7B7653EF207E74F31E6323CAA5BB16BF769165D89D5E4B47DA18C9FE13C163D0E0FF0035353151D73455ADBD431EFB6F47B0CD6944B8591843E9230A0A4F2E3DE3AF7D51429DA16F448AB358159AA8A1451450047B5BE9B56A9D3ABB6B4E21A77B66DC42D7C9385713FD2555EDD3F6083A6ED4D5BA037BADA38A967E2715D54A3D49A28ACA5760752A21B43D1C9D5566DF8E948B8C50551D5CB7FBD04F71E9DC71E34514356A80E1ECA345AADB14DFAE2C944B7D2531DB58C169BEA48E855FA79D32A8A2B22A91AC3145145318689B062DC62391263087D874616DAC642856F4A424000600E94514019A28A2800A28A2803FFFD9")

                    val writeTask = WriteIDCardTask(ctx.card, cardProtocol!!.reader, App.localStorage, App.pinStorage, idCardData, ctx.card.issuer.privateDataKey,
                            object : CardProtocol.Notifications {
                                override fun onReadProgress(cardProtocol: CardProtocol?, progress: Int) {
                                    LOG.i("WriteIDCardTask", "onReadProgress: ${progress}%")
                                }

                                override fun onReadWait(msec: Int) {
                                }

                                override fun onReadStart(cardProtocol: CardProtocol?) {
                                    LOG.i("WriteIDCardTask", "onReadStart")
                                    rlProgressBar?.post { rlProgressBar?.visibility = View.VISIBLE }

                                }

                                override fun onReadCancel() {
                                    LOG.i("WriteIDCardTask", "onReadCancel")
                                    rlProgressBar?.postDelayed({
                                        try {
                                            rlProgressBar?.visibility = View.GONE
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }, 500)

                                }

                                override fun onReadFinish(cardProtocol: CardProtocol?) {
                                    LOG.i("WriteIDCardTask", "onReadFinish")
                                    rlProgressBar?.postDelayed({
                                        try {
                                            rlProgressBar?.visibility = View.GONE
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }, 500)

                                }

                                override fun onReadAfterRequest() {
                                }

                                override fun onReadBeforeRequest(timeout: Int) {
                                }
                            })
                    writeTask.start()
                    return
                }
                //TODO end
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