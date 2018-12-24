package com.tangem.presentation.activity

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import com.tangem.Constant
import com.tangem.data.fingerprint.StartFingerprintReaderTask
import com.tangem.tangemcard.android.reader.NfcManager
import com.tangem.data.fingerprint.FingerprintHelper
import com.tangem.domain.wallet.TangemContext
import com.tangem.tangemcard.android.data.PINStorage
import com.tangem.tangemcard.data.TangemCard
import com.tangem.tangemcard.data.loadFromBundle
import com.tangem.tangemcard.data.EXTRA_TANGEM_CARD
import com.tangem.tangemcard.data.EXTRA_TANGEM_CARD_UID
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_pin_request.*
import kotlinx.android.synthetic.main.layout_pin_buttons.*
import java.io.IOException

class PinRequestActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, FingerprintHelper.FingerprintHelperListener {

    companion object {
        fun callingIntent(context: Activity, mode: String): Intent {
            val intent = Intent(context, PinRequestActivity::class.java)
            intent.putExtra(Constant.EXTRA_MODE, mode)
            return intent
        }

        fun callingIntentRequestPin(context: Activity, mode: String, ctx: TangemContext, newPIN: String): Intent {
            val intent = Intent(context, PinRequestActivity::class.java)
            intent.putExtra(Constant.EXTRA_MODE, mode)
            intent.putExtra(Constant.EXTRA_NEW_PIN, newPIN)
            ctx.saveToIntent(intent)
            return intent
        }

        fun callingIntentRequestPin2(context: Activity, mode: String, ctx: TangemContext, newPIN2: String): Intent {
            val intent = Intent(context, PinRequestActivity::class.java)
            intent.putExtra(Constant.EXTRA_MODE, mode)
            intent.putExtra(Constant.EXTRA_NEW_PIN_2, newPIN2)
            ctx.saveToIntent(intent)
            return intent
        }

        fun callingIntentRequestPin2(context: Activity, mode: String, ctx: TangemContext): Intent {
            val intent = Intent(context, PinRequestActivity::class.java)
            intent.putExtra(Constant.EXTRA_MODE, mode)
            ctx.saveToIntent(intent)
            return intent
        }

        fun callingIntentConfirmPin(context: Activity, mode: String, newPIN: String): Intent {
            val intent = Intent(context, PinRequestActivity::class.java)
            intent.putExtra(Constant.EXTRA_MODE, mode)
            intent.putExtra(Constant.EXTRA_NEW_PIN, newPIN)
            return intent
        }

        fun callingIntentConfirmPin2(context: Activity, mode: String, newPIN2: String): Intent {
            val intent = Intent(context, PinRequestActivity::class.java)
            intent.putExtra(Constant.EXTRA_MODE, mode)
            intent.putExtra(Constant.EXTRA_NEW_PIN_2, newPIN2)
            return intent
        }
    }

    lateinit var mode: Mode
    private var allowFingerprint = false
    private var nfcManager: NfcManager? = null

    var startFingerprintReaderTask: StartFingerprintReaderTask? = null

    private var fingerprintManager: FingerprintManager? = null
    private var fingerprintHelper: FingerprintHelper? = null

    enum class Mode {
        RequestPIN, RequestPIN2, RequestNewPIN, RequestNewPIN2, ConfirmNewPIN, ConfirmNewPIN2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_request)

        nfcManager = NfcManager(this, this)

        mode = Mode.valueOf(intent.getStringExtra(Constant.EXTRA_MODE))

        if (mode == Mode.RequestNewPIN)
            if (PINStorage.haveEncryptedPIN()) {
                allowFingerprint = true
                tvPinPrompt.setText(R.string.enter_new_pin_or_use_fingerprint_scanner)
            } else
                tvPinPrompt.setText(R.string.enter_new_pin)
        else if (mode == Mode.ConfirmNewPIN)
            tvPinPrompt.setText(R.string.confirm_new_pin)
        else if (mode == Mode.RequestPIN)
            if (PINStorage.haveEncryptedPIN()) {
                allowFingerprint = true
                tvPinPrompt.setText(R.string.enter_pin_or_use_fingerprint_scanner)
            } else
                tvPinPrompt.setText(R.string.enter_pin)
        else if (mode == Mode.RequestNewPIN2)
            if (PINStorage.haveEncryptedPIN2()) {
                allowFingerprint = true
                tvPinPrompt.setText(R.string.enter_new_pin_2_or_use_fingerprint_scanner)
            } else
                tvPinPrompt.setText(R.string.enter_new_pin_2)
        else if (mode == Mode.ConfirmNewPIN2)
            tvPinPrompt.setText(R.string.confirm_new_pin_2)
        else if (mode == Mode.RequestPIN2) {
            val uid = intent.getStringExtra(EXTRA_TANGEM_CARD_UID)
            val card = TangemCard(uid)
            card.loadFromBundle(intent.getBundleExtra(EXTRA_TANGEM_CARD))

            if (card.PIN2 == TangemCard.PIN2_Mode.DefaultPIN2 || card.PIN2 == TangemCard.PIN2_Mode.Unchecked) {
                // if we know PIN2 or not try default previously - use it
                PINStorage.setPIN2(PINStorage.getDefaultPIN2())
                setResult(Activity.RESULT_OK)
                finish()
                return
            }

            if (PINStorage.haveEncryptedPIN2()) {
                allowFingerprint = true
                tvPinPrompt.setText(R.string.enter_pin_2_or_use_fingerprint_scanner)
            } else
                tvPinPrompt.setText(R.string.enter_pin_2)
        }

        if (!allowFingerprint)
            tvPinPrompt.visibility = View.GONE
        else
            tvPinPrompt.visibility = View.VISIBLE

        // set listeners
        btn0.setOnClickListener { buttonClick(btn0) }
        btn1.setOnClickListener { buttonClick(btn1) }
        btn2.setOnClickListener { buttonClick(btn2) }
        btn3.setOnClickListener { buttonClick(btn3) }
        btn4.setOnClickListener { buttonClick(btn4) }
        btn5.setOnClickListener { buttonClick(btn5) }
        btn6.setOnClickListener { buttonClick(btn6) }
        btn7.setOnClickListener { buttonClick(btn7) }
        btn8.setOnClickListener { buttonClick(btn8) }
        btn9.setOnClickListener { buttonClick(btn9) }
        btnBackspace.setOnClickListener {
            val s = tvPin!!.text.toString()
            if (s.isNotEmpty())
                tvPin!!.text = s.substring(0, s.length - 1)
        }
        btnContinue.setOnClickListener { doContinue() }
    }

    override fun onPause() {
        super.onPause()

        if (fingerprintHelper != null)
            fingerprintHelper!!.cancel()

        if (startFingerprintReaderTask != null) {
            startFingerprintReaderTask!!.cancel(true)
            startFingerprintReaderTask = null
        }

        nfcManager!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (fingerprintHelper != null)
            fingerprintHelper!!.cancel()

        if (startFingerprintReaderTask != null) {
            startFingerprintReaderTask!!.cancel(true)
            startFingerprintReaderTask = null
        }

        nfcManager!!.onStop()
    }

    override fun onResume() {
        super.onResume()
        nfcManager!!.onResume()
        if (allowFingerprint)
            startFingerprintReader()
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            Log.w(javaClass.name, "Ignore discovered tag!")
            nfcManager!!.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun authenticationFailed(error: String) {
        doLog(error)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun authenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
        doLog("Authentication succeeded!")
        val cipher = result.cryptoObject.cipher

        if (mode == Mode.RequestNewPIN || mode == Mode.ConfirmNewPIN) {
            val resultData = Intent()
            val pin = PINStorage.loadEncryptedPIN(cipher)
            resultData.putExtra("newPIN", pin)
            resultData.putExtra("confirmPIN", pin)
            setResult(Activity.RESULT_OK, resultData)
            finish()
        } else if (mode == Mode.RequestNewPIN2 || mode == Mode.ConfirmNewPIN2) {
            val resultData = Intent()
            val pin = PINStorage.loadEncryptedPIN2(cipher)
            resultData.putExtra("newPIN2", pin)
            resultData.putExtra("confirmPIN2", pin)
            setResult(Activity.RESULT_OK, resultData)
            finish()
        } else if (mode == Mode.RequestPIN) {
            PINStorage.loadEncryptedPIN(cipher)
            setResult(Activity.RESULT_OK)
        } else if (mode == Mode.RequestPIN2) {
            PINStorage.loadEncryptedPIN2(cipher)
            setResult(Activity.RESULT_OK)
        }

        finish()
    }

    fun doLog(text: String) {
        //        Log.e("FP", text);
    }

    @SuppressLint("SetTextI18n")
    private fun buttonClick(button: Button) {
        tvPin!!.text = tvPin!!.text.toString() + button.text as String
    }

    @SuppressLint("NewApi")
    private fun testFingerPrintSettings(): Boolean {
        doLog("Testing Fingerprint Settings")

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        fingerprintManager = getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

        if (!keyguardManager.isKeyguardSecure) {
            doLog("User hasn't enabled Lock Screen")
            return false
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            doLog("User hasn't granted permission to use Fingerprint")
            return false
        }

        if (!fingerprintManager!!.hasEnrolledFingerprints()) {
            doLog("User hasn't registered any fingerprints")
            return false
        }

        doLog("Fingerprint authentication is set.\n")

        return true
    }

    private fun startFingerprintReader() {
        if (!testFingerPrintSettings())
            return

        if (!allowFingerprint)
            return

        fingerprintHelper = FingerprintHelper(this@PinRequestActivity)
        startFingerprintReaderTask = StartFingerprintReaderTask(this, fingerprintManager, fingerprintHelper)
        startFingerprintReaderTask!!.execute(null as Void?)
    }

    private fun doContinue() {
        if (startFingerprintReaderTask != null)
            return

        // reset errors.
        tvPin!!.error = null

        // store values at the time of the login attempt.
        val pin = tvPin!!.text.toString()

        var cancel = false
        var focusView: View? = null

        if (mode == Mode.ConfirmNewPIN) {
            if (pin != intent.getStringExtra("newPIN")) {
                tvPin!!.error = getString(R.string.error_pin_confirmation_failed)
                focusView = tvPin
                cancel = true
            }
        } else if (mode == Mode.ConfirmNewPIN2) {
            if (pin != intent.getStringExtra("newPIN2")) {
                tvPin!!.error = getString(R.string.error_pin_confirmation_failed)
                focusView = tvPin
                cancel = true
            }
        } else {
            if (TextUtils.isEmpty(pin)) {
                tvPin!!.error = getString(R.string.error_empty_pin)
                focusView = tvPin
                cancel = true
            }
        }

        if (cancel)
            focusView!!.requestFocus()
        else {
            if (mode == Mode.RequestNewPIN || mode == Mode.ConfirmNewPIN) {
                val resultData = Intent()
                resultData.putExtra("newPIN", pin)
                if (mode == Mode.ConfirmNewPIN)
                    resultData.putExtra("confirmPIN", pin)

                setResult(Activity.RESULT_OK, resultData)
                finish()
            } else if (mode == Mode.RequestNewPIN2 || mode == Mode.ConfirmNewPIN2) {
                val resultData = Intent()
                resultData.putExtra("newPIN2", pin)
                if (mode == Mode.ConfirmNewPIN2)
                    resultData.putExtra("confirmPIN2", pin)

                setResult(Activity.RESULT_OK, resultData)
                finish()
            } else if (mode == Mode.RequestPIN) {
                PINStorage.setUserPIN(pin)
                setResult(Activity.RESULT_OK)
                finish()
            } else if (mode == Mode.RequestPIN2) {
                PINStorage.setPIN2(pin)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

}