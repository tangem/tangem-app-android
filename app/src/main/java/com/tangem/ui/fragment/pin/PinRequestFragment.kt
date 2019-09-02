package com.tangem.ui.fragment.pin

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.core.app.ActivityCompat
import com.tangem.Constant
import com.tangem.data.fingerprint.FingerprintHelper
import com.tangem.data.fingerprint.StartFingerprintReaderTask
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_sdk.android.data.PINStorage
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD_UID
import com.tangem.tangem_sdk.data.loadFromBundle
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.fragment.BaseFragment
import com.tangem.util.LOG
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.fragment_pin_request.*
import kotlinx.android.synthetic.main.layout_pin_buttons.*
import java.io.IOException

class PinRequestFragment : BaseFragment(), NfcAdapter.ReaderCallback, FingerprintHelper.FingerprintHelperListener {
    companion object {
        val TAG: String = PinRequestFragment::class.java.simpleName

        fun callingIntent(mode: String): Bundle {
            val bundle = Bundle()
            bundle.putString(Constant.EXTRA_MODE, mode)
            return bundle
        }

        fun callingIntentRequestPin(mode: String, ctx: TangemContext, newPIN: String): Bundle {
            val bundle = Bundle()
            bundle.putString(Constant.EXTRA_MODE, mode)
            bundle.putString(Constant.EXTRA_NEW_PIN, newPIN)
            ctx.saveToBundle(bundle)
            return bundle
        }

        fun callingIntentRequestPin2(mode: String, ctx: TangemContext, newPIN2: String): Bundle {
            val bundle = Bundle()
            bundle.putString(Constant.EXTRA_MODE, mode)
            bundle.putString(Constant.EXTRA_NEW_PIN_2, newPIN2)
            ctx.saveToBundle(bundle)
            return bundle
        }

        fun callingIntentRequestPin2(mode: String, ctx: TangemContext): Bundle {
            val bundle = Bundle()
            bundle.putString(Constant.EXTRA_MODE, mode)
            ctx.saveToBundle(bundle)
            return bundle
        }

        fun callingIntentConfirmPin(mode: String, newPIN: String): Bundle {
            val bundle = Bundle()
            bundle.putString(Constant.EXTRA_MODE, mode)
            bundle.putString(Constant.EXTRA_NEW_PIN, newPIN)
            return bundle
        }

        fun callingIntentConfirmPin2(mode: String, newPIN2: String): Bundle {
            val bundle = Bundle()
            bundle.putString(Constant.EXTRA_MODE, mode)
            bundle.putString(Constant.EXTRA_NEW_PIN_2, newPIN2)
            return bundle
        }
    }

    override val layoutId = R.layout.fragment_pin_request

    val mode: Mode by lazy { Mode.valueOf(arguments?.getString(Constant.EXTRA_MODE) ?: "") }

    private var allowFingerprint = false
    var startFingerprintReaderTask: StartFingerprintReaderTask? = null
    private var fingerprintManager: FingerprintManager? = null
    private var fingerprintHelper: FingerprintHelper? = null

    enum class Mode {
        RequestPIN, RequestPIN2, RequestNewPIN, RequestNewPIN2, ConfirmNewPIN, ConfirmNewPIN2
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            val uid = arguments?.getString(EXTRA_TANGEM_CARD_UID)
            val card = TangemCard(uid)
            card.loadFromBundle(arguments?.getBundle(EXTRA_TANGEM_CARD) ?: Bundle())

            if (card.PIN2 == TangemCard.PIN2_Mode.DefaultPIN2 || card.PIN2 == TangemCard.PIN2_Mode.Unchecked) {
                // if we know PIN2 or not try default previously - use it
                PINStorage.setPIN2(PINStorage.getDefaultPIN2())
                navigateBackWithResult(Activity.RESULT_OK)
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
        fingerprintHelper?.cancel()

        if (startFingerprintReaderTask != null) {
            startFingerprintReaderTask!!.cancel(true)
            startFingerprintReaderTask = null
        }
    }

    override fun onStop() {
        super.onStop()
        fingerprintHelper?.cancel()

        if (startFingerprintReaderTask != null) {
            startFingerprintReaderTask!!.cancel(true)
            startFingerprintReaderTask = null
        }
    }

    override fun onResume() {
        super.onResume()
        if (allowFingerprint)
            startFingerprintReader()
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            Log.w(javaClass.name, "Ignore discovered tag!")
            (activity as MainActivity).nfcManager.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun authenticationFailed(error: String) {
        LOG.w(TAG, error)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun authenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
        LOG.i(TAG, "Authentication succeeded!")
        val cipher = result.cryptoObject.cipher

        if (mode == Mode.RequestNewPIN || mode == Mode.ConfirmNewPIN) {
            val resultData = Bundle()
            val pin = PINStorage.loadEncryptedPIN(cipher)
            resultData.putString(Constant.EXTRA_NEW_PIN, pin)
            resultData.putString(Constant.EXTRA_CONFIRM_PIN, pin)
            navigateBackWithResult(Activity.RESULT_OK, resultData)
        } else if (mode == Mode.RequestNewPIN2 || mode == Mode.ConfirmNewPIN2) {
            val resultData = Bundle()
            val pin = PINStorage.loadEncryptedPIN2(cipher)
            resultData.putString(Constant.EXTRA_NEW_PIN_2, pin)
            resultData.putString(Constant.EXTRA_CONFIRM_PIN_2, pin)
            navigateBackWithResult(Activity.RESULT_OK, resultData)
        } else if (mode == Mode.RequestPIN) {
            PINStorage.loadEncryptedPIN(cipher)
            navigateBackWithResult(Activity.RESULT_OK)
        } else if (mode == Mode.RequestPIN2) {
            PINStorage.loadEncryptedPIN2(cipher)
            navigateBackWithResult(Activity.RESULT_OK)
        }
    }

    private fun buttonClick(button: Button) {
        tvPin.text = tvPin.text.toString() + button.text.toString()
    }

    @SuppressLint("NewApi")
    private fun testFingerPrintSettings(): Boolean {
        LOG.i(TAG, "Testing Fingerprint SettingsFragment")

        val keyguardManager = context?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        fingerprintManager = context?.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

        if (!keyguardManager.isKeyguardSecure) {
            LOG.i(TAG, "User hasn't enabled Lock Screen")
            return false
        }

        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            LOG.i(TAG, "User hasn't granted permission to use Fingerprint")
            return false
        }

        if (!fingerprintManager!!.hasEnrolledFingerprints()) {
            LOG.i(TAG, "User hasn't registered any fingerprints")
            return false
        }

        LOG.i(TAG, "Fingerprint authentication is set.\n")

        return true
    }

    private fun startFingerprintReader() {
        if (!testFingerPrintSettings())
            return

        if (!allowFingerprint)
            return

        fingerprintHelper = FingerprintHelper(this@PinRequestFragment)
        startFingerprintReaderTask = StartFingerprintReaderTask(
                this@PinRequestFragment, fingerprintManager, fingerprintHelper)
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
            if (pin != arguments?.getString(Constant.EXTRA_NEW_PIN)) {
                tvPin!!.error = getString(R.string.error_pin_confirmation_failed)
                focusView = tvPin
                cancel = true
            }
        } else if (mode == Mode.ConfirmNewPIN2) {
            if (pin != arguments?.getString(Constant.EXTRA_NEW_PIN_2)) {
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
                val resultData = Bundle()
                resultData.putString(Constant.EXTRA_NEW_PIN, pin)
                if (mode == Mode.ConfirmNewPIN)
                    resultData.putString(Constant.EXTRA_CONFIRM_PIN, pin)

                navigateBackWithResult(Activity.RESULT_OK, resultData)

            } else if (mode == Mode.RequestNewPIN2 || mode == Mode.ConfirmNewPIN2) {
                val resultData = Bundle()
                resultData.putString(Constant.EXTRA_NEW_PIN_2, pin)
                if (mode == Mode.ConfirmNewPIN2)
                    resultData.putString(Constant.EXTRA_CONFIRM_PIN_2, pin)
                navigateBackWithResult(Activity.RESULT_OK, resultData)

            } else if (mode == Mode.RequestPIN) {
                PINStorage.setUserPIN(pin)
                navigateBackWithResult(Activity.RESULT_OK)

            } else if (mode == Mode.RequestPIN2) {
                PINStorage.setPIN2(pin)
                navigateBackWithResult(Activity.RESULT_OK)
            }
        }
    }
}