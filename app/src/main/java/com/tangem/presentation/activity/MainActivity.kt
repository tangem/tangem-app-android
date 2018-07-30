package com.tangem.presentation.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.PopupMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.widget.RelativeLayout
import android.widget.Toast
import com.scottyab.rootbeer.RootBeer
import com.tangem.data.nfc.ReadCardInfoTask
import com.tangem.domain.cardReader.CardProtocol
import com.tangem.domain.cardReader.FW
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.*
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.RootFoundDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.util.CommonUtil
import com.tangem.util.PhoneUtility
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications, PopupMenu.OnMenuItemClickListener {

    companion object {
        val TAG: String = MainActivity::class.java.simpleName

        private const val REQUEST_CODE_SEND_EMAIL = 3
        private const val REQUEST_CODE_ENTER_PIN_ACTIVITY = 2
        private const val REQUEST_CODE_SHOW_CARD_ACTIVITY = 1

        const val EXTRA_LAST_DISCOVERED_TAG = "extra_last_tag"

        fun commonInit(context: Context) {
            if (PINStorage.needInit())
                PINStorage.Init(context)

            if (LastSignStorage.needInit())
                LastSignStorage.Init(context)

            if (Issuer.needInit())
                Issuer.Init(context)

            if (FW.needInit())
                FW.Init(context)
        }
    }

    private var zipFile: File? = null
    private val onNFCReaderCallback: NfcAdapter.ReaderCallback? = null
    private val onCardsClean: OnCardsClean? = null
    private var antenna: DeviceNFCAntennaLocation? = null
    private var mNfcManager: NfcManager? = null
    private var readCardInfoTask: ReadCardInfoTask? = null
    private var unsuccessReadCount = 0
    private var lastTag: Tag? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action)) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null && onNFCReaderCallback != null)
                onNFCReaderCallback.onTagDiscovered(tag)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        mNfcManager = NfcManager(this, this)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        commonInit(applicationContext)

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
            if (tag != null && onNFCReaderCallback != null) {
                onNFCReaderCallback.onTagDiscovered(tag)
            }
        }

        // check if root device
        val rootBeer = RootBeer(this)
        if (rootBeer.isRootedWithoutBusyBoxCheck && !BuildConfig.DEBUG)
            RootFoundDialog().show(fragmentManager, RootFoundDialog.TAG)

        // set listeners
        fab.setOnClickListener { this.showMenu(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SEND_EMAIL) {
            if (zipFile != null) {
                zipFile!!.delete()
                zipFile = null
            }
        }

        if (requestCode == REQUEST_CODE_ENTER_PIN_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK && lastTag != null)
                onTagDiscovered(lastTag!!)
            else
                ReadCardInfoTask.resetLastReadInfo()
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
                showSavePinActivity()
                return true
            }

            R.id.managePIN2 -> {
                showSavePin2Activity()
                return true
            }

            R.id.cleanCards -> {
                onCardsClean!!.doClean()
                llTapPrompt.visibility = View.VISIBLE
                return true
            }

            R.id.about -> {
                showLogoActivity()
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

            readCardInfoTask = ReadCardInfoTask(this, mNfcManager, isoDep, this)
            readCardInfoTask!!.start()

//            Log.i(TAG, "onTagDiscovered " + Arrays.toString(tag.getId()));

        } catch (e: Exception) {
            e.printStackTrace()
            mNfcManager!!.notifyReadResult(false)
        }
    }

    public override fun onResume() {
        super.onResume()
        animate()
        ReadCardInfoTask.resetLastReadInfo()
        mNfcManager!!.onResume()
    }

    public override fun onPause() {
        mNfcManager!!.onPause()
        if (readCardInfoTask != null) {
            readCardInfoTask!!.cancel(true)
        }
        super.onPause()
    }

    public override fun onStop() {
        // dismiss enable NFC dialog
        mNfcManager!!.onStop()
        if (readCardInfoTask != null) {
            readCardInfoTask!!.cancel(true)
        }
        super.onStop()
    }

    override fun OnReadStart(cardProtocol: CardProtocol) {
        rlProgressBar.post { rlProgressBar.visibility = View.VISIBLE }
    }

    override fun OnReadProgress(protocol: CardProtocol, progress: Int) {

    }

    override fun OnReadFinish(cardProtocol: CardProtocol?) {
        readCardInfoTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                mNfcManager!!.notifyReadResult(true)
                rlProgressBar.post {
                    rlProgressBar.visibility = View.GONE

                    val cardInfo = Bundle()
                    cardInfo.putString("UID", cardProtocol.card.uid)
                    val bCard = Bundle()
                    cardProtocol.card.SaveToBundle(bCard)
                    cardInfo.putBundle("Card", bCard)

                    val uid = cardInfo.getString("UID")
                    val card = TangemCard(uid)
                    card.LoadFromBundle(cardInfo.getBundle("Card"))

                    val intent: Intent
                    intent = if (card.status == TangemCard.Status.Empty)
                        Intent(this, EmptyWalletActivity::class.java)
                    else if (card.status == TangemCard.Status.Loaded)
                        Intent(this, LoadedWalletActivity::class.java)
                    else if (card.status == TangemCard.Status.NotPersonalized || card.status == TangemCard.Status.Purged)
                        return@post
                    else
                        Intent(this, LoadedWalletActivity::class.java)

                    intent.putExtra(EXTRA_LAST_DISCOVERED_TAG, lastTag)
                    intent.putExtras(cardInfo)
                    startActivityForResult(intent, REQUEST_CODE_SHOW_CARD_ACTIVITY)
                }

            } else {
                // remove last UIDs because of error and no card read
                rlProgressBar.post {
                    Toast.makeText(this, R.string.try_to_scan_again, Toast.LENGTH_SHORT).show()
                    unsuccessReadCount++

                    if (cardProtocol.error is CardProtocol.TangemException_InvalidPIN)
                        doEnterPIN()
                    else {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported)
                            if (!NoExtendedLengthSupportDialog.allReadyShowed)
                                NoExtendedLengthSupportDialog().show(fragmentManager, NoExtendedLengthSupportDialog.TAG)

                        lastTag = null
                        ReadCardInfoTask.resetLastReadInfo()
                        mNfcManager!!.notifyReadResult(false)
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

    override fun OnReadCancel() {
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

    override fun OnReadWait(msec: Int) {
        WaitSecurityDelayDialog.OnReadWait(Objects.requireNonNull(this), msec)
    }

    override fun OnReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(Objects.requireNonNull(this), timeout)
    }

    override fun OnReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(Objects.requireNonNull(this))
    }

    interface OnCardsClean {
        fun doClean()
    }

    private fun showLogoActivity() {
        val intent = Intent(baseContext, LogoActivity::class.java)
        intent.putExtra(LogoActivity.TAG, true)
        intent.putExtra(LogoActivity.EXTRA_AUTO_HIDE, false)
        startActivity(intent)
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

    private fun showSavePinActivity() {
        val intent = Intent(baseContext, SavePINActivity::class.java)
        intent.putExtra("PIN2", false)
        startActivity(intent)
    }

    private fun showSavePin2Activity() {
        val intent = Intent(baseContext, SavePINActivity::class.java)
        intent.putExtra("PIN2", true)
        startActivity(intent)
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

    private fun doEnterPIN() {
        val intent = Intent(this, RequestPINActivity::class.java)
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN.toString())
        startActivityForResult(intent, REQUEST_CODE_ENTER_PIN_ACTIVITY)
    }

}