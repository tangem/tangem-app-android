package com.tangem.presentation.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.tangem.App
import com.tangem.tangemcard.tasks.PurgeTask
import com.tangem.tangemcard.reader.CardProtocol
import com.tangem.tangemcard.android.reader.NfcManager
import com.tangem.domain.wallet.TangemContext
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.tangemcard.android.reader.NfcReader
import com.tangem.tangemcard.data.asBundle
import com.tangem.tangemcard.util.Util
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_purge.*

class PurgeActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        val TAG: String = PurgeActivity::class.java.simpleName
        const val RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER
    }

    private lateinit var ctx: TangemContext
    private lateinit var nfcManager: NfcManager
    private var purgeTask: PurgeTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purge)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        nfcManager = NfcManager(this, this)

//        MainActivity.commonInit(applicationContext)

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        tvCardID.text = ctx.card!!.cidDescription
        progressBar.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar.visibility = View.INVISIBLE
    }

    public override fun onResume() {
        super.onResume()
        nfcManager?.onResume()
    }

    public override fun onPause() {
        nfcManager?.onPause()
        purgeTask?.cancel(true)
        super.onPause()
    }

    public override fun onStop() {
        // dismiss enable NFC dialog
        nfcManager?.onStop()
        purgeTask?.cancel(true)
        super.onStop()
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)
                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)
//            Log.v(TAG, "UID: $sUID")

            if (sUID == ctx.card!!.uid) {
                isoDep.timeout = ctx.card!!.pauseBeforePIN2 + 65000
                purgeTask = PurgeTask(ctx.card, NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, this)
                purgeTask!!.start()
            } else {
                //               this Log.d(TAG, "Mismatch card UID (" + sUID + " instead of " + card.getUID() + ")");
                nfcManager?.ignoreTag(isoDep.tag)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.onReadWait(this, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(this)
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        progressBar.post {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 5
        }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        purgeTask = null

        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                progressBar!!.post {
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.GREEN)
                    val intent = Intent()
                    intent.putExtra("UID", cardProtocol.card.uid)
                    intent.putExtra("Card", cardProtocol.card.asBundle)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            } else {
                if (cardProtocol.error is CardProtocol.TangemException_InvalidPIN) {
                    progressBar!!.post {
                        progressBar!!.progress = 100
                        progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                    progressBar!!.postDelayed({
                        try {
                            progressBar!!.progress = 0
                            progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                            progressBar!!.visibility = View.INVISIBLE
                            val intent = Intent()
                            intent.putExtra("UID", cardProtocol.card.uid)
                            intent.putExtra("Card", cardProtocol.card.asBundle)
                            intent.putExtra("message", getString(R.string.cannot_erase_wallet))
                            setResult(RESULT_INVALID_PIN, intent)
                            finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                } else {
                    progressBar!!.post {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allReadyShowed)
                                NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                        } else
                            Toast.makeText(baseContext, R.string.try_to_scan_again, Toast.LENGTH_LONG).show()

                        progressBar!!.progress = 100
                        progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                }
            }
        }

        progressBar!!.postDelayed({
            try {
                progressBar!!.progress = 0
                progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                progressBar!!.visibility = View.INVISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {
        progressBar!!.post { progressBar!!.progress = progress }
    }

    override fun onReadCancel() {
        purgeTask = null
        progressBar!!.postDelayed({
            try {
                progressBar!!.progress = 0
                progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                progressBar!!.visibility = View.INVISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

}