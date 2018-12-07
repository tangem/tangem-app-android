package com.tangem.presentation.activity

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
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.PopupMenu
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.widget.RelativeLayout
import android.widget.Toast
import com.scottyab.rootbeer.RootBeer
import com.tangem.App
import com.tangem.Constant
import com.tangem.data.Logger
import com.tangem.tangemcard.data.local.PINStorage
import com.tangem.data.network.ServerApiCommon
import com.tangem.tangemcard.data.nfc.DeviceNFCAntennaLocation
import com.tangem.tangemcard.tasks.ReadCardInfoTask
import com.tangem.di.Navigator
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemContext
import com.tangem.tangemcard.reader.CardProtocol
import com.tangem.tangemcard.data.local.Firmwares
import com.tangem.tangemcard.reader.NfcManager
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.RootFoundDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.tangemcard.data.Issuer
import com.tangem.tangemcard.data.TangemCard
import com.tangem.util.CommonUtil
import com.tangem.util.PhoneUtility
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*
import javax.inject.Inject

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications, PopupMenu.OnMenuItemClickListener {

    companion object {
        val TAG: String = MainActivity::class.java.simpleName

        fun callingIntent(context: Context) = Intent(context, MainActivity::class.java)

//        fun commonInit(context: Context) {
//            if (PINStorage.needInit())
//                PINStorage.init(context)
//
//            if (Issuer.needInit())
//                Issuer.init(context)
//
//            if (Firmwares.needInit())
//                Firmwares.init(context)
//        }
    }

    private var nfcManager: NfcManager? = null
    private var zipFile: File? = null
    private var antenna: DeviceNFCAntennaLocation? = null
    private var unsuccessReadCount = 0
    private var lastTag: Tag? = null
    private var readCardInfoTask: ReadCardInfoTask? = null
    private var onNfcReaderCallback: NfcAdapter.ReaderCallback? = null

    @Inject
    internal lateinit var navigator: Navigator

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

        App.getNavigatorComponent().inject(this)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        nfcManager = NfcManager(this, this)

        verifyPermissions()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

//        commonInit(applicationContext)

        setNfcAdapterReaderCallback(this)

        rippleBackgroundNfc.startRippleAnimation()

        antenna = DeviceNFCAntennaLocation()
        antenna!!.getAntennaLocation()

        // set card orientation
        when (antenna!!.orientation) {
            DeviceNFCAntennaLocation.CARD_ORIENTATION_HORIZONTAL -> {
                ivHandCardHorizontal.visibility = View.VISIBLE
                ivHandCardVertical.visibility = View.GONE
            }

            DeviceNFCAntennaLocation.CARD_ORIENTATION_VERTICAL -> {
                ivHandCardVertical.visibility = View.VISIBLE
                ivHandCardHorizontal.visibility = View.GONE
            }
        }

        // set card z position
        when (antenna!!.z) {
            DeviceNFCAntennaLocation.CARD_ON_BACK -> llHand.elevation = 0.0f
            DeviceNFCAntennaLocation.CARD_ON_FRONT -> llHand.elevation = 30.0f
        }

        // set phone name
        if (antenna!!.fullName != "")
            tvNFCHint.text = String.format(getString(R.string.scan_banknote), antenna!!.fullName)
        else
            tvNFCHint.text = String.format(getString(R.string.scan_banknote), getString(R.string.phone))

        animate()

        // NFC
        val intent = intent
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action)) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null && onNfcReaderCallback != null) {
                onNfcReaderCallback!!.onTagDiscovered(tag)
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
                if (response.isNullOrEmpty()) return@setLastVersionListener
                val responseVersionName = response.trim(' ', '\n', '\r', '\t')
                val responseBuildVersion = responseVersionName.split('.').last()
                val appBuildVersion = BuildConfig.VERSION_NAME.split('.').last()
                if (responseBuildVersion.toInt() > appBuildVersion.toInt()) Toast.makeText(this, "There is a new application version: $responseVersionName", Toast.LENGTH_LONG).show()
            } catch (E: Exception) {
                E.printStackTrace()
            }
        }
        apiHelper.requestLastVersion()
    }

    private fun verifyPermissions() {
        NfcManager.verifyPermissions(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e("QRScanActivity", "User hasn't granted permission to use camera")
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
//                        Log.e(TAG, String.format("Collect %d log bytes", f.length()));
                        CommonUtil.sendEmail(this, zipFile, TAG, "Logs", PhoneUtility.getDeviceInfo(), arrayOf(f))
                    } else {
//                        Log.e(TAG, "Can't create temporaly log file");
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
                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))

//            Log.e(TAG, "setTimeout(" + String.valueOf(1000 + 3000 * unsuccessReadCount) + ")");
            if (unsuccessReadCount < 2) {
                isoDep.timeout = 2000 + 5000 * unsuccessReadCount
            } else {
                isoDep.timeout = 90000
            }
            lastTag = tag

            readCardInfoTask = ReadCardInfoTask(this, nfcManager, isoDep, this)
            readCardInfoTask!!.start()

//            Log.i(TAG, "onTagDiscovered " + Arrays.toString(tag.getId()));

        } catch (e: Exception) {
            e.printStackTrace()
            nfcManager!!.notifyReadResult(false)
        }
    }

    public override fun onResume() {
        super.onResume()
        animate()
        ReadCardInfoTask.resetLastReadInfo()
        nfcManager!!.onResume()
    }

    public override fun onPause() {
        nfcManager!!.onPause()
        if (readCardInfoTask != null) {
            readCardInfoTask!!.cancel(true)
        }
        super.onPause()
    }

    public override fun onStop() {
        // dismiss enable NFC dialog
        nfcManager!!.onStop()
        if (readCardInfoTask != null) {
            readCardInfoTask!!.cancel(true)
        }
        super.onStop()
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar.post { rlProgressBar.visibility = View.VISIBLE }
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {

    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        readCardInfoTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                nfcManager!!.notifyReadResult(true)
                rlProgressBar.post {
                    rlProgressBar.visibility = View.GONE

//                    // TODO - ??? remove save and load???
                    val cardInfo = Bundle()
                    cardInfo.putString("UID", cardProtocol.card.uid)
                    val bCard = Bundle()
                    cardProtocol.card.saveToBundle(bCard)
                    cardInfo.putBundle("Card", bCard)

                    val uid = cardInfo.getString("UID")
                    val card = TangemCard(uid)
                    card.loadFromBundle(cardInfo.getBundle("Card"))

                    val ctx = TangemContext(card)
                    when {
                        card.status == TangemCard.Status.Loaded -> lastTag?.let {
                            val engineCoin = CoinEngineFactory.create(ctx) ?: throw CardProtocol.TangemException("Can't create CoinEngine!")
                            engineCoin.defineWallet()
                            //mCard.setWallet(Blockchain.calculateWalletAddress(mCard, pkUncompressed));

                            navigator.showLoadedWallet(this, it, ctx)
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
                        nfcManager!!.notifyReadResult(false)
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
        readCardInfoTask = null
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
        WaitSecurityDelayDialog.OnReadWait(Objects.requireNonNull(this), msec)
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

    private fun animate() {
        val lp = llHand.layoutParams as RelativeLayout.LayoutParams
        val lp2 = llNfc.layoutParams as RelativeLayout.LayoutParams
        val dp = resources.displayMetrics.density
        val lm = dp * (69 + antenna!!.x * 75)
        lp.topMargin = (dp * (-100 + antenna!!.y * 250)).toInt()
        lp2.topMargin = (dp * (-125 + antenna!!.y * 250)).toInt()
        llNfc.layoutParams = lp2

        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                lp.leftMargin = (lm * interpolatedTime).toInt()
                llHand.layoutParams = lp
            }
        }
        a.duration = 2000
        a.interpolator = DecelerateInterpolator()
        llHand.startAnimation(a)
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