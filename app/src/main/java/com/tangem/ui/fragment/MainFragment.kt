package com.tangem.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcV
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.App
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.data.Logger
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.reader.CardProtocol
import com.tangem.tangem_card.reader.TLV
import com.tangem.tangem_card.reader.TLVException
import com.tangem.tangem_card.reader.TLVList
import com.tangem.tangem_card.tasks.CustomReadCardTask
import com.tangem.tangem_card.tasks.ReadCardInfoTask
import com.tangem.tangem_sdk.android.data.PINStorage
import com.tangem.tangem_sdk.android.nfc.NfcDeviceAntennaLocation
import com.tangem.tangem_sdk.android.reader.NfcReader
import com.tangem.tangem_sdk.android.reader.NfcVReader
import com.tangem.tangem_sdk.android.reader.ReadResult
import com.tangem.tangem_sdk.android.reader.ReadSlixTagTask
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD_UID
import com.tangem.tangem_sdk.data.loadFromBundle
import com.tangem.tangem_sdk.data.saveToBundle
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.ui.fragment.pin.PinRequestFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.util.*
import com.tangem.util.extensions.colorFrom
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import com.tangem.wallet.xlmTag.XlmTagEngine
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.layout_touch_card.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainFragment : BaseFragment(), NavigationResultListener, NfcAdapter.ReaderCallback,
        CardProtocol.Notifications, androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener,
        PopupMenu.OnMenuItemClickListener {

    companion object {
        fun newInstance() = MainFragment()
        val TAG: String = MainFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fragment_main
    private lateinit var viewModel: MainViewModel
    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation
    private var unsuccessReadCount = 0
    private var task: CustomReadCardTask? = null
    private var lastTag: Tag? = null
    private var zipFile: File? = null
    private var unknownBlockchain = false

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO
    private val scope = CoroutineScope(coroutineContext)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rippleBackgroundNfc.startRippleAnimation()

//        navigateToDestination(R.id.action_main_to_emptyIdFragment)
        // init NFC Antenna
        nfcDeviceAntenna = NfcDeviceAntennaLocation(context!!, ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc)
        nfcDeviceAntenna.init()

        // set phone name
        if (nfcDeviceAntenna.fullName != "")
            tvNFCHint.text = String.format(getString(R.string.main_screen_scan_card), nfcDeviceAntenna.fullName)
        else
            tvNFCHint.text = String.format(getString(R.string.main_screen_scan_card), getString(R.string.main_screen_phone))

        // set listeners
//        ivMenu.setOnClickListener { showMenu(it) }

        val toolbar = tb_main
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)


        setCardStoreLink()

        FirebaseAnalytics.getInstance(requireActivity())
                .logEvent(AnalyticsEvent.READY_TO_SCAN.event, bundleOf())
    }

    private fun setCardStoreLink() {
        val text = getString(R.string.main_screen_visit_store, getString(R.string.main_screen_store_address))
        val spannable = SpannableString(text)
        spannable.setSpan(
                ForegroundColorSpan(requireContext().colorFrom(R.color.colorAccent)),
                text.indexOf(getString(R.string.main_screen_store_address)),
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvBuyCards?.setText(spannable, TextView.BufferType.SPANNABLE)
        llShoppingView?.setOnClickListener {
            val uri = Uri.parse("https://www.tangemcards.com")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
//
//        // show snackbar about new version app
//        viewModel.getVersionName().observe(this, Observer { text ->
//            (activity as MainActivity).toastHelper.showSnackbarUpdateVersion(requireContext(), cl, text)
//        })

        val intent = activity?.intent
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action)) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            onTagDiscovered(tag)
        }
    }

    override fun onResume() {
        super.onResume()
        nfcDeviceAntenna.animate()
    }

    override fun onPause() {
        task?.cancel(true)
        super.onPause()
    }

    override fun onStop() {
        task?.cancel(true)
        super.onStop()
    }

    override fun onTagDiscovered(tag: Tag) {
        if (unknownBlockchain) {
            (activity as MainActivity).nfcManager.ignoreTag(tag)
            return
        }

        NfcV.get(tag)?.let { onNfcVDiscovered(it, tag.id) }

        try {
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)
            if (unsuccessReadCount < 2)
                isoDep.timeout = 2000 + 5000 * unsuccessReadCount
            else
                isoDep.timeout = 90000

            lastTag = tag

            val terminalKeys = viewModel.getTerminalKeys()
            PINStorage.setTerminalPrivateKey(terminalKeys[Constant.TERMINAL_PRIVATE_KEY])
            PINStorage.setTerminalPublicKey(terminalKeys[Constant.TERMINAL_PUBLIC_KEY])

            task = ReadCardInfoTask(NfcReader((activity as MainActivity).nfcManager, isoDep),
                    App.localStorage, App.pinStorage, this)
            task?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            (activity as MainActivity).nfcManager.notifyReadResult(false)
        }
    }


    private fun onNfcVDiscovered(nfcV: NfcV, uid: ByteArray) {
        scope.launch {
            when (val readResult = ReadSlixTagTask(NfcVReader(nfcV)).read()) {
                is ReadResult.Failure -> (activity as MainActivity).nfcManager.notifyReadResult(false)
                is ReadResult.Success -> {
                    try {
                        val tlvs = readResult.tlvs
                        val cardDataTlv = TLVList.fromBytes((tlvs.getTLV(TLV.Tag.TAG_CardData)).Value)
                        Log.v(TAG, "\n" + tlvs.getParsedTLVs(""))
                        val card = TangemCard(uid.toString())
                        card.batch = cardDataTlv.getTLV(TLV.Tag.TAG_Batch).asHexString
                        card.setIssuer(cardDataTlv.getTLV(TLV.Tag.TAG_Issuer_ID).Value.toString(), null)
                        card.blockchainID = Blockchain.StellarTag.id
                        card.walletPublicKey = tlvs.getTLV(TLV.Tag.TAG_Wallet_PublicKey).Value
                        card.status = TangemCard.Status.Loaded
                        card.tagSignature = tlvs.getTLV(TLV.Tag.TAG_Signature).Value

                        val ctx = TangemContext(card)
                        val engineCoin = XlmTagEngine(ctx)
                        engineCoin.defineWallet()
                        launch(Dispatchers.Main) {

                            val bundle = Bundle()
                            bundle.putParcelable(Constant.EXTRA_LAST_DISCOVERED_TAG, lastTag)
                            ctx.saveToBundle(bundle)
                            navigateForResult(Constant.REQUEST_CODE_SHOW_CARD_ACTIVITY,
                                    R.id.action_main_to_tagFragment, bundle)
                        }
                    } catch (e: TLVException) {
                        e.printStackTrace()
                        Log.v(TAG, e.message)
                    }
                }
            }
        }
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar?.post { rlProgressBar?.visibility = View.VISIBLE }
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {

    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        task = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                (activity as MainActivity).nfcManager.notifyReadResult(true)
                rlProgressBar?.post {
                    rlProgressBar?.visibility = View.GONE

                    // TODO - ??? remove save and load???
                    val cardInfo = Bundle()
                    val bCard = Bundle()

                    cardInfo.putString(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                    cardInfo.putBundle(EXTRA_TANGEM_CARD, bCard)
                    cardProtocol.card.saveToBundle(bCard)

                    val uid = cardInfo.getString(EXTRA_TANGEM_CARD_UID)
                    val card = TangemCard(uid)
                    cardInfo.getBundle(EXTRA_TANGEM_CARD)?.let { card.loadFromBundle(it) }

                    val terminalKeys = viewModel.getTerminalKeys()
                    card.terminalPrivateKey = terminalKeys[Constant.TERMINAL_PRIVATE_KEY]
                    card.terminalPublicKey = terminalKeys[Constant.TERMINAL_PUBLIC_KEY]

                    val ctx = TangemContext(card)

                    FirebaseAnalytics.getInstance(requireActivity())
                            .logEvent(AnalyticsEvent.CARD_IS_SCANNED.event, Analytics.setCardData(ctx))

                    when {
                        card.status == TangemCard.Status.Loaded -> lastTag?.let {
                            val engineCoin = CoinEngineFactory.create(ctx)
                            if (engineCoin != null) {
                                if (card.isIDCard) {
                                    val bundle = Bundle()
                                    bundle.putParcelable(Constant.EXTRA_LAST_DISCOVERED_TAG, lastTag)
                                    ctx.saveToBundle(bundle)
                                    navigateToDestination(R.id.action_main_to_idFragment, bundle)
                                } else {
                                    engineCoin.defineWallet()
                                    val bundle = Bundle()
                                    bundle.putParcelable(Constant.EXTRA_LAST_DISCOVERED_TAG, lastTag)
                                    ctx.saveToBundle(bundle)
                                    navigateForResult(Constant.REQUEST_CODE_SHOW_CARD_ACTIVITY,
                                            R.id.action_main_to_loadedWalletFragment, bundle)
                                }
                            } else {
                                showUnkownBlockchainWarning()
                            }
                        }
                        card.status == TangemCard.Status.Empty -> {
                            val engineCoin = CoinEngineFactory.create(ctx)
                            if (engineCoin != null) {
                                val bundle = Bundle().apply { ctx.saveToBundle(this) }
                                navigateToDestination(R.id.action_main_to_emptyWalletFragment, bundle)
                            } else {
                                showUnkownBlockchainWarning()
                            }
                        }
                        card.status == TangemCard.Status.Purged -> Toast.makeText(context, R.string.main_screen_erased_wallet, Toast.LENGTH_SHORT).show()
                        card.status == TangemCard.Status.NotPersonalized -> Toast.makeText(context, R.string.main_screen_not_personalized, Toast.LENGTH_SHORT).show()
                        else -> {

//                            val bundle = Bundle()
//                            bundle.putParcelable(Constant.EXTRA_LAST_DISCOVERED_TAG, lastTag)
//                            ctx.saveToBundle(bundle)
//                            navController.navigate(R.id.loadedWallet, bundle)

//                            lastTag?.let { navigator.showLoadedWallet(this, it, ctx) }
                        }
                    }
                }

            } else {
                FirebaseCrashlytics.getInstance().recordException(cardProtocol.error)
                // remove last UIDs because of error and no card read
                rlProgressBar.post {
                    context?.let { Toast.makeText(it, R.string.general_notification_scan_again, Toast.LENGTH_SHORT).show() }
                    unsuccessReadCount++

                    if (cardProtocol.error is CardProtocol.TangemException_InvalidPIN) {
                        val data = Bundle().apply { putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN.toString()) }
                        navigateForResult(Constant.REQUEST_CODE_ENTER_PIN_ACTIVITY,
                                R.id.action_main_to_pinRequestFragment, data)
                    } else {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported)
                            if (!NoExtendedLengthSupportDialog.allReadyShowed)
                                NoExtendedLengthSupportDialog().show(activity!!.supportFragmentManager, NoExtendedLengthSupportDialog.TAG)

                        lastTag = null
                        ReadCardInfoTask.resetLastReadInfo()
                        (activity as MainActivity).nfcManager.notifyReadResult(false)
                    }
                }
            }
        }

        rlProgressBar?.postDelayed({ rlProgressBar?.visibility = View.GONE }, 500)
    }

    private fun showUnkownBlockchainWarning() {
        unknownBlockchain = true
        AlertDialog.Builder(context)
                .setTitle(R.string.dialog_warning)
                .setMessage(R.string.alert_unknown_blockchain)
                .setPositiveButton(R.string.general_ok) { _, _ -> }
                .setOnDismissListener { unknownBlockchain = false }
                .create()
                .show()
    }

    override fun onReadCancel() {
        task = null
        ReadCardInfoTask.resetLastReadInfo()
        rlProgressBar?.postDelayed({ rlProgressBar?.visibility = View.GONE }, 500)
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        when (requestCode) {
            Constant.REQUEST_CODE_SEND_EMAIL -> {
                if (zipFile != null) {
                    zipFile!!.delete()
                    zipFile = null
                }
            }
            Constant.REQUEST_CODE_ENTER_PIN_ACTIVITY -> {
                if (resultCode == Activity.RESULT_OK && lastTag != null)
                    onTagDiscovered(lastTag!!)
                else
                    ReadCardInfoTask.resetLastReadInfo()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        if (BuildConfig.DEBUG) {
            for (i in 0 until menu.size()) menu.getItem(i).isVisible = true
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sendLogs -> {
                var f: File? = null
                try {
                    f = Logger.collectLogs(activity)
                    if (f != null) {
                        LOG.e(TAG, String.format("Collect %d log bytes", f.length()))
                        CommonUtil.sendEmail(activity, zipFile, TAG, "Logs", UtilHelper.getDeviceInfo(), arrayOf(f))
                    } else {
                        LOG.e(TAG, "Can't create temporarily log file")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (f != null && f.exists())
                        f.delete()
                }
                return true
            }
            R.id.managePIN -> {
                navigateToDestination(R.id.action_main_to_pinSaveFragment,
                        bundleOf(Constant.EXTRA_PIN2 to false))
                return true
            }

            R.id.managePIN2 -> {
                navigateToDestination(R.id.action_main_to_pinSaveFragment,
                        bundleOf(Constant.EXTRA_PIN2 to true))
                return true
            }

            R.id.settings -> {
                navigateToDestination(R.id.action_main_to_settingsFragment)
                return true
            }

            R.id.about -> {
                val bundle = Bundle().apply { putBoolean(Constant.EXTRA_AUTO_HIDE, false) }
                navigateToDestination(R.id.action_main_to_logoFragment, bundle)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.onReadWait(Objects.requireNonNull(activity) as AppCompatActivity?, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(Objects.requireNonNull(activity) as AppCompatActivity?, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(Objects.requireNonNull(activity))
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return onOptionsItemSelected(item!!)
    }

    private fun showMenu(v: View) {
        val popup = PopupMenu(context, v)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_main, popup.menu)

        if (BuildConfig.DEBUG) {
            popup.menu.findItem(R.id.managePIN).isEnabled = true
            popup.menu.findItem(R.id.managePIN2).isEnabled = true
            popup.menu.findItem(R.id.sendLogs).isVisible = true
        }

        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

}