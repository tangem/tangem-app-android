@file:Suppress("ObsoleteExperimentalCoroutines")

package com.tangem.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.scottyab.rootbeer.RootBeer
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
import com.tangem.data.network.ServerApiCommon
import com.tangem.di.Navigator
import com.tangem.di.ToastHelper
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.TangemContext
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.RootFoundDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.util.CommonUtil
import com.tangem.util.LOG
import com.tangem.util.PhoneUtility
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_touch_card.*
import java.io.File
import java.util.*
import javax.inject.Inject

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications, PopupMenu.OnMenuItemClickListener {

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
        fun callingIntent(context: Context) = Intent(context, MainActivity::class.java)
    }

    @Inject
    internal lateinit var navigator: Navigator
    @Inject
    internal lateinit var toastHelper: ToastHelper

    private lateinit var nfcManager: NfcManager

    private var zipFile: File? = null
    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation
    private var unsuccessReadCount = 0
    private var lastTag: Tag? = null
    private var task: CustomReadCardTask? = null
    private var onNfcReaderCallback: NfcAdapter.ReaderCallback? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action)) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null && onNfcReaderCallback != null)
                onNfcReaderCallback!!.onTagDiscovered(tag)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        App.navigatorComponent.inject(this)
        App.toastHelperComponent.inject(this)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        verifyPermissions()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        setNfcAdapterReaderCallback(this)

        rippleBackgroundNfc.startRippleAnimation()

        // init NFC Antenna
        nfcDeviceAntenna = NfcDeviceAntennaLocation(this, ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc)
        nfcDeviceAntenna.init()

        // set phone name
        if (nfcDeviceAntenna.fullName != "")
            tvNFCHint.text = String.format(getString(R.string.scan_banknote), nfcDeviceAntenna.fullName)
        else
            tvNFCHint.text = String.format(getString(R.string.scan_banknote), getString(R.string.phone))

        // NFC
        val intent = intent
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action)) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null && onNfcReaderCallback != null) {
                onNfcReaderCallback?.onTagDiscovered(tag)
            }
        }

        // check if root device
        val rootBeer = RootBeer(this)
        if (rootBeer.isRootedWithoutBusyBoxCheck && !BuildConfig.DEBUG)
            RootFoundDialog().show(supportFragmentManager, RootFoundDialog.TAG)

        // set listeners
        fab.setOnClickListener { showMenu(it) }

        val apiHelper = ServerApiCommon()
        apiHelper.setLastVersionListener { response ->
            try {
                if (response.isNullOrEmpty())
                    return@setLastVersionListener
                val responseVersionName = response.trim(' ', '\n', '\r', '\t')
                val responseBuildVersion = responseVersionName.split('.').last()
                val appBuildVersion = BuildConfig.VERSION_NAME.split('.').last()
                if (responseBuildVersion.toInt() > appBuildVersion.toInt())
                    if (BuildConfig.FLAVOR.equals(Constant.FLAVOR_TANGEM_ACCESS))
                        toastHelper.showSnackbarUpdateVersion(this, cl, responseVersionName)
            } catch (E: Exception) {
                E.printStackTrace()
            }
        }
        apiHelper.requestLastVersion()
    }

    private fun verifyPermissions() {
        NfcManager.verifyPermissions(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), Constant.REQUEST_CODE_REQUEST_CAMERA_PERMISSIONS)
        }
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

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        if (BuildConfig.DEBUG) {
            for (i in 0 until menu.size()) menu.getItem(i).isVisible = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.sendLogs -> {
                var f: File? = null
                try {
                    f = Logger.collectLogs(this)
                    if (f != null) {
                        LOG.e(TAG, String.format("Collect %d log bytes", f.length()))
                        CommonUtil.sendEmail(this, zipFile, TAG, "Logs", PhoneUtility.getDeviceInfo(), arrayOf(f))
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
                navigator.showPinSave(this, false)
                return true
            }

            R.id.managePIN2 -> {
                navigator.showPinSave(this, true)
                return true
            }

            R.id.about -> {
                navigator.showLogo(this, false)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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

    public override fun onResume() {
        super.onResume()
        nfcDeviceAntenna.animate()
        ReadCardInfoTask.resetLastReadInfo()
    }

    public override fun onPause() {
        task?.cancel(true)
        super.onPause()
    }

    public override fun onStop() {
        task?.cancel(true)
        super.onStop()
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar.post { rlProgressBar.visibility = View.VISIBLE }
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {

    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        task = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                nfcManager.notifyReadResult(true)
                rlProgressBar.post {
                    rlProgressBar.visibility = View.GONE

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
                                navigator.showLoadedWallet(this, it, ctx)
                            }
                        }
                        card.status == TangemCard.Status.Empty -> navigator.showEmptyWallet(this, ctx)
                        card.status == TangemCard.Status.Purged -> Toast.makeText(this, R.string.erased_wallet, Toast.LENGTH_SHORT).show()
                        card.status == TangemCard.Status.NotPersonalized -> Toast.makeText(this, R.string.not_personalized, Toast.LENGTH_SHORT).show()
                        else -> lastTag?.let { navigator.showLoadedWallet(this, it, ctx) }
                    }
                }

            } else {
                // remove last UIDs because of error and no card read
                rlProgressBar.post {
                    Toast.makeText(this, R.string.try_to_scan_again, Toast.LENGTH_SHORT).show()
                    unsuccessReadCount++

                    if (cardProtocol.error is CardProtocol.TangemException_InvalidPIN)
                        navigator.showPinRequest(this, PinRequestActivity.Mode.RequestPIN.toString())
                    else {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported)
                            if (!NoExtendedLengthSupportDialog.allReadyShowed)
                                NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)

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

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.onReadWait(Objects.requireNonNull(this), msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(Objects.requireNonNull(this), timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(Objects.requireNonNull(this))
    }

    private fun setNfcAdapterReaderCallback(callback: NfcAdapter.ReaderCallback) {
        onNfcReaderCallback = callback
    }

    private fun showMenu(v: View) {
        val popup = PopupMenu(this, v)
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