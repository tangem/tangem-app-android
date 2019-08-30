package com.tangem.ui.fragment

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tangem.App
import com.tangem.Constant
import com.tangem.card_android.android.nfc.NfcDeviceAntennaLocation
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.card_android.android.reader.NfcReader
import com.tangem.card_android.data.EXTRA_TANGEM_CARD
import com.tangem.card_android.data.EXTRA_TANGEM_CARD_UID
import com.tangem.card_android.data.loadFromBundle
import com.tangem.card_android.data.saveToBundle
import com.tangem.card_common.data.TangemCard
import com.tangem.card_common.reader.CardProtocol
import com.tangem.card_common.tasks.CustomReadCardTask
import com.tangem.card_common.tasks.ReadCardInfoTask
import com.tangem.data.Logger
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.activity.PinRequestActivity
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.util.CommonUtil
import com.tangem.util.LOG
import com.tangem.util.UtilHelper
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.layout_touch_card.*
import kotlinx.android.synthetic.main.main_fragment.*
import java.io.File
import java.util.*

class MainFragment : Fragment(), NfcAdapter.ReaderCallback, CardProtocol.Notifications, androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener, PopupMenu.OnMenuItemClickListener {

    companion object {
        fun newInstance() = MainFragment()
        val TAG: String = MainFragment::class.java.simpleName
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation
    private var unsuccessReadCount = 0
    private lateinit var nfcManager: NfcManager
    private var task: CustomReadCardTask? = null
    private var lastTag: Tag? = null
    private var zipFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcManager = NfcManager(activity!!, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rippleBackgroundNfc.startRippleAnimation()

        // init NFC Antenna
        nfcDeviceAntenna = NfcDeviceAntennaLocation(context!!, ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc)
        nfcDeviceAntenna.init()

        // set phone name
        if (nfcDeviceAntenna.fullName != "")
            tvNFCHint.text = String.format(getString(R.string.main_screen_scan_banknote), nfcDeviceAntenna.fullName)
        else
            tvNFCHint.text = String.format(getString(R.string.main_screen_scan_banknote), getString(R.string.main_screen_phone))

        // set listeners
        fab.setOnClickListener { showMenu(it) }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        // show snackbar about new version app
        viewModel.getVersionName().observe(this, Observer { text ->
            (activity as MainActivity).toastHelper.showSnackbarUpdateVersion(context!!, cl, text)
        })

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
        try {
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)
            if (unsuccessReadCount < 2)
                isoDep.timeout = 2000 + 5000 * unsuccessReadCount
            else
                isoDep.timeout = 90000

            lastTag = tag

            task = ReadCardInfoTask(NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, this)
            task?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            nfcManager.notifyReadResult(false)
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
                nfcManager.notifyReadResult(true)
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

                    val ctx = TangemContext(card)
                    when {
                        card.status == TangemCard.Status.Loaded -> lastTag?.let {
                            val engineCoin = CoinEngineFactory.create(ctx)
                            if (engineCoin != null) {
                                engineCoin.defineWallet()

//                                val bundle = Bundle()
//                                bundle.putParcelable(Constant.EXTRA_LAST_DISCOVERED_TAG, lastTag)
//                                ctx.saveToBundle(bundle)
//                                (activity as MainActivity).navController.navigate(R.id.loadedWallet, bundle)

                                (activity as MainActivity).navigator.showLoadedWallet(activity!!, it, ctx)
                            }
                        }
                        card.status == TangemCard.Status.Empty -> (activity as MainActivity).navigator.showEmptyWallet(activity!!, ctx)
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
                // remove last UIDs because of error and no card read
                rlProgressBar.post {
                    Toast.makeText(context, R.string.general_notification_scan_again, Toast.LENGTH_SHORT).show()
                    unsuccessReadCount++

                    if (cardProtocol.error is CardProtocol.TangemException_InvalidPIN)
                        (activity as MainActivity).navigator.showPinRequest(activity!!, PinRequestActivity.Mode.RequestPIN.toString())
                    else {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported)
                            if (!NoExtendedLengthSupportDialog.allReadyShowed)
                                NoExtendedLengthSupportDialog().show(activity!!.supportFragmentManager, NoExtendedLengthSupportDialog.TAG)

                        lastTag = null
                        ReadCardInfoTask.resetLastReadInfo()
                        nfcManager.notifyReadResult(false)
                    }
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
        task = null
        ReadCardInfoTask.resetLastReadInfo()
        rlProgressBar.postDelayed({
            try {
                rlProgressBar.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
        super.onActivityResult(requestCode, resultCode, data)
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
                (activity as MainActivity).navigator.showPinSave(activity!!, false)
//                (activity as MainActivity).navController.navigate(R.id.pinSaveActivity)
                return true
            }

            R.id.managePIN2 -> {
                (activity as MainActivity).navigator.showPinSave(activity!!, true)
//                (activity as MainActivity).navController.navigate(R.id.pinSaveActivity)
                return true
            }

            R.id.settings -> {
                (activity as MainActivity).navigator.showSettings(activity!!)
//                (activity as MainActivity).navController.navigate(R.id.settingsActivity)
                return true
            }

            R.id.about -> {
                (activity as MainActivity).navigator.showLogo(activity!!, false)
//                (activity as MainActivity).navController.navigate(R.id.logoActivity)
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