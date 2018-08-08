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
import com.tangem.data.nfc.PurgeTask
import com.tangem.domain.cardReader.CardProtocol
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.TangemCard
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.util.Util
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_purge.*

class PurgeActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        val TAG: String = PurgeActivity::class.java.simpleName
        const val RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER
    }

    private var mCard: TangemCard? = null
    private var mNfcManager: NfcManager? = null
    private var purgeTask: PurgeTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purge)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        mNfcManager = NfcManager(this, this)

        MainActivity.commonInit(applicationContext)

        mCard = TangemCard(intent.getStringExtra("UID"))
        mCard!!.loadFromBundle(intent.extras!!.getBundle("Card"))

        tvCardID.text = mCard!!.cidDescription
        progressBar.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar.visibility = View.INVISIBLE
    }

    public override fun onResume() {
        super.onResume()
        mNfcManager!!.onResume()
    }

    public override fun onPause() {
        mNfcManager!!.onPause()
        if (purgeTask != null) {
            purgeTask!!.cancel(true)
        }
        super.onPause()
    }

    public override fun onStop() {
        // dismiss enable NFC dialog
        mNfcManager!!.onStop()
        if (purgeTask != null) {
            purgeTask!!.cancel(true)
        }
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

            if (sUID == mCard!!.uid) {
                isoDep.timeout = mCard!!.pauseBeforePIN2 + 65000
                purgeTask = PurgeTask(this, mCard, mNfcManager, isoDep, this)
                purgeTask!!.start()
            } else {
                //               this Log.d(TAG, "Mismatch card UID (" + sUID + " instead of " + mCard.getUID() + ")");
                mNfcManager!!.ignoreTag(isoDep.tag)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.OnReadWait(this, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(this)
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        progressBar!!.post {
            progressBar!!.visibility = View.VISIBLE
            progressBar!!.progress = 5
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
                                NoExtendedLengthSupportDialog().show(fragmentManager, NoExtendedLengthSupportDialog.TAG)
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