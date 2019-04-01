package com.tangem.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tangem.App
import com.tangem.Constant
import com.tangem.domain.wallet.TangemContext
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialogNew
import com.tangem.ui.event.DeletingWalletFinish
import com.tangem.card_android.android.nfc.NfcDeviceAntennaLocation
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.card_android.android.reader.NfcReader
import com.tangem.card_android.data.asBundle
import com.tangem.card_common.reader.CardProtocol
import com.tangem.card_common.tasks.PurgeTask
import com.tangem.card_common.util.Util
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_purge.*
import kotlinx.android.synthetic.main.layout_touch_card.*
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import com.tangem.card_android.data.EXTRA_TANGEM_CARD
import com.tangem.card_android.data.EXTRA_TANGEM_CARD_UID

class PurgeActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {
    companion object {
        val TAG: String = PurgeActivity::class.java.simpleName

        fun callingIntent(context: Context, ctx: TangemContext): Intent {
            val intent = Intent(context, PurgeActivity::class.java)
            ctx.saveToIntent(intent)
            return intent
        }

        const val RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER
    }

    @Inject
    internal lateinit var waitSecurityDelayDialogNew: WaitSecurityDelayDialogNew

    private lateinit var nfcManager: NfcManager
    private lateinit var ctx: TangemContext

    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation

    private var purgeTask: PurgeTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purge)

        App.navigatorComponent.inject(this)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        // init NFC Antenna
        nfcDeviceAntenna = NfcDeviceAntennaLocation(this, ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc)
        nfcDeviceAntenna.init()

        tvCardID.text = ctx.card.cidDescription
        progressBar.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar.visibility = View.INVISIBLE
    }

    public override fun onStop() {
        purgeTask?.cancel(true)
        super.onStop()
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)
            if (sUID == ctx.card.uid) {
                isoDep.timeout = ctx.card.pauseBeforePIN2 + 65000
                purgeTask = PurgeTask(ctx.card, NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, this)
                purgeTask?.start()
            } else {
                nfcManager.ignoreTag(isoDep.tag)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.onReadWait(this, msec)

//        val readWait = ReadWait()
//        readWait.msec = msec
//        EventBus.getDefault().post(readWait)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)

//        if (!waitSecurityDelayDialogNew.isAdded)
//            waitSecurityDelayDialogNew.show(supportFragmentManager, WaitSecurityDelayDialogNew.TAG)
//
//
//        val readBeforeRequest = ReadBeforeRequest()
//        readBeforeRequest.timeout = timeout
//        EventBus.getDefault().post(readBeforeRequest)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(this)

//        val readAfterRequest = ReadAfterRequest()
//        EventBus.getDefault().post(readAfterRequest)
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar.post { rlProgressBar.visibility = View.VISIBLE }

        progressBar.post {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 5
        }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        purgeTask = null

        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar.post { rlProgressBar.visibility = View.GONE }

                progressBar?.post {
                    progressBar?.progress = 100
                    progressBar?.progressTintList = ColorStateList.valueOf(Color.GREEN)

                    val intent = Intent()
                    intent.putExtra(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                    intent.putExtra(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                    setResult(Activity.RESULT_OK, intent)

                    EventBus.getDefault().post(DeletingWalletFinish())

                    finish()
                }
            } else {
                if (cardProtocol.error is CardProtocol.TangemException_InvalidPIN) {
                    progressBar?.post {
                        progressBar?.progress = 100
                        progressBar?.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                    progressBar?.postDelayed({
                        try {
                            progressBar?.progress = 0
                            progressBar?.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                            progressBar?.visibility = View.INVISIBLE

                            val intent = Intent()
                            intent.putExtra(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                            intent.putExtra(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                            intent.putExtra(Constant.EXTRA_MESSAGE, getString(R.string.cannot_erase_wallet))
                            setResult(RESULT_INVALID_PIN, intent)
                            finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                } else {
                    progressBar?.post {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allReadyShowed)
                                NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                        } else
                            Toast.makeText(baseContext, R.string.try_to_scan_again, Toast.LENGTH_LONG).show()

                        progressBar?.progress = 100
                        progressBar?.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                }
            }
        }

        rlProgressBar?.postDelayed({
            try {
                rlProgressBar.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)

        progressBar?.postDelayed({
            try {
                progressBar?.progress = 0
                progressBar?.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                progressBar?.visibility = View.INVISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {
        progressBar?.post { progressBar?.progress = progress }
    }

    override fun onReadCancel() {
        purgeTask = null
        progressBar?.postDelayed({
            try {
                progressBar?.progress = 0
                progressBar?.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                progressBar?.visibility = View.INVISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

}