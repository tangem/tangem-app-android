package com.tangem.presentation.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.tangem.App
import com.tangem.Constant
import com.tangem.tangemcard.tasks.SwapPINTask
import com.tangem.tangemcard.reader.CardProtocol
import com.tangem.tangemcard.android.reader.NfcManager
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.tangemcard.android.reader.NfcReader
import com.tangem.tangemcard.data.*
import com.tangem.tangemcard.util.Util
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_pin_swap.*

class PinSwapActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        val TAG: String = PinSwapActivity::class.java.simpleName

        fun callingIntent(context: Context, newPIN: String, newPIN2: String): Intent {
            val intent = Intent(context, PinSwapActivity::class.java)
            intent.putExtra(Constant.EXTRA_NEW_PIN, newPIN)
            intent.putExtra(Constant.EXTRA_NEW_PIN_2, newPIN2)
            return intent
        }

        const val RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER
    }

    private lateinit var nfcManager: NfcManager
    private var card: TangemCard? = null

    private var newPIN: String? = null
    private var newPIN2: String? = null

    private var progressBar: ProgressBar? = null

    private var swapPinTask: SwapPINTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_swap)

//        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        card = TangemCard(intent.getStringExtra(EXTRA_TANGEM_CARD_UID))
        card!!.loadFromBundle(intent.extras!!.getBundle(EXTRA_TANGEM_CARD))

        newPIN = intent.getStringExtra(Constant.EXTRA_NEW_PIN)
        newPIN2 = intent.getStringExtra(Constant.EXTRA_NEW_PIN_2)

        tvCardID.text = card!!.cidDescription

        progressBar = findViewById(R.id.progressBar)
        progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar!!.visibility = View.INVISIBLE
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)
                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)
//            Log.v(TAG, "UID: $sUID")

            if (sUID == card!!.uid) {
                isoDep.timeout = card!!.pauseBeforePIN2 + 65000
                swapPinTask = SwapPINTask(card, NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, this, newPIN, newPIN2)
                swapPinTask!!.start()
            } else {
//                Log.d(TAG, "Mismatch card UID (" + sUID + " instead of " + mCard!!.uid + ")")
                nfcManager!!.ignoreTag(isoDep.tag)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onResume() {
        super.onResume()
        nfcManager!!.onResume()
    }

    public override fun onPause() {
        nfcManager!!.onPause()
        if (swapPinTask != null)
            swapPinTask!!.cancel(true)
        super.onPause()
    }

    public override fun onStop() {
        // dismiss enable NFC dialog
        nfcManager!!.onStop()
        if (swapPinTask != null)
            swapPinTask!!.cancel(true)
        super.onStop()
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        progressBar?.post {
            progressBar?.visibility = View.VISIBLE
            progressBar?.progress = 5
        }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        swapPinTask = null

        if (cardProtocol != null) {
            when {
                cardProtocol.error == null -> progressBar!!.post {
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.GREEN)
                    val intent = Intent()
                    intent.putExtra("UID", cardProtocol.card.uid)
                    intent.putExtra("Card", cardProtocol.card.asBundle)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
                cardProtocol.error is CardProtocol.TangemException_InvalidPIN -> {
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
                            intent.putExtra("message", "Cannot change PIN(s). Make sure you enter correct PIN2!")
                            intent.putExtra("UID", cardProtocol.card.uid)
                            intent.putExtra("Card", cardProtocol.card.asBundle)
                            setResult(RESULT_INVALID_PIN, intent)
                            finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                }
                else -> progressBar!!.post {
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                        if (!NoExtendedLengthSupportDialog.allReadyShowed) {
                            NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                        }
                    } else {
                        Toast.makeText(baseContext, R.string.try_to_scan_again, Toast.LENGTH_SHORT).show()
                    }
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)
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
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {
        progressBar!!.post { progressBar!!.progress = progress }
    }

    override fun onReadCancel() {
        swapPinTask = null

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

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.onReadWait(this, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(this)
    }

}