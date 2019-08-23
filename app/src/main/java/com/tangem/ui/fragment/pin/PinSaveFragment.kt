package com.tangem.ui.fragment.pin

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.tangem.Constant
import com.tangem.card_android.android.data.PINStorage
import com.tangem.data.fingerprint.ConfirmWithFingerprintTask
import com.tangem.data.fingerprint.FingerprintHelper
import com.tangem.ui.fragment.BaseFragment
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_pin_save.*
import kotlinx.android.synthetic.main.layout_pin_buttons.*
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class PinSaveFragment : BaseFragment(), FingerprintHelper.FingerprintHelperListener {
    companion object {
        val TAG: String = PinSaveFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fragment_pin_save

    private var confirmWithFingerprintTask: ConfirmWithFingerprintTask? = null
    private var keyStore: KeyStore? = null
    private var cipher: Cipher? = null
    var fingerprintManager: FingerprintManager? = null
    var cryptoObject: FingerprintManager.CryptoObject? = null
    var fingerprintHelper: FingerprintHelper? = null
    private var usePIN2 = false
    private var onConfirmAction: OnConfirmAction? = null
    var dFingerPrintConfirmation: Dialog? = null

    private enum class OnConfirmAction {
        Save, DeleteEncryptedAndSave, Delete
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usePIN2 = arguments?.getBoolean(Constant.EXTRA_PIN2, false) ?: false

        if (usePIN2)
            tvPinPrompt.text = getString(R.string.enter_pin2_and_use_fingerprint_to_save_it)
        else
            tvPinPrompt.text = getString(R.string.enter_pin_and_use_fingerprint_to_save_it)

        if (usePIN2) {
            cbUseFingerprint.isChecked = true
            cbUseFingerprint.isEnabled = false
        } else {
            cbUseFingerprint.isChecked = PINStorage.haveEncryptedPIN()
            cbUseFingerprint.isEnabled = true
        }

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
            val s = tvPin.text.toString()
            if (s.isNotEmpty())
                tvPin.text = s.substring(0, s.length - 1)
        }
        btnSavePIN.setOnClickListener { doSavePIN() }
        btnDeletePIN.setOnClickListener { doDeletePIN() }
    }

    override fun onPause() {
        super.onPause()
        if (fingerprintHelper != null)
            fingerprintHelper!!.cancel()

        if (confirmWithFingerprintTask != null)
            confirmWithFingerprintTask!!.cancel(true)
    }

    override fun onStop() {
        super.onStop()
        if (fingerprintHelper != null)
            fingerprintHelper!!.cancel()

        if (confirmWithFingerprintTask != null)
            confirmWithFingerprintTask!!.cancel(true)
    }

    override fun authenticationFailed(error: String) {

    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun authenticationSucceeded(result: FingerprintManager.AuthenticationResult) {

        cipher = result.cryptoObject.cipher

        when (onConfirmAction) {
            OnConfirmAction.Save -> {
                val textToEncrypt = tvPin.text.toString()
                if (usePIN2) {
                    PINStorage.saveEncryptedPIN2(cipher, textToEncrypt)
                } else {
                    PINStorage.saveEncryptedPIN(cipher, textToEncrypt)
                }
            }

            OnConfirmAction.Delete -> {
                if (usePIN2) {
                    PINStorage.deleteEncryptedPIN2()
                } else {
                    PINStorage.deleteEncryptedPIN()
                    PINStorage.deletePIN()
                }
                tvPin.text = ""
            }

            OnConfirmAction.DeleteEncryptedAndSave -> if (usePIN2) {
                PINStorage.deleteEncryptedPIN2()
                PINStorage.saveEncryptedPIN2(cipher, tvPin.text.toString())
            } else {
                PINStorage.deleteEncryptedPIN()
                PINStorage.savePIN(tvPin.text.toString())
            }
        }

        dFingerPrintConfirmation!!.dismiss()
        navigateUp()
    }

    fun getKeyStore(): Boolean {
        try {
            keyStore = KeyStore.getInstance(Constant.KEYSTORE)
            // create empty keystore
            keyStore!!.load(null)
            return true
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun createNewKey(forceCreate: Boolean): Boolean {
        try {
            if (forceCreate)
                keyStore!!.deleteEntry(Constant.KEY_ALIAS)

            if (!keyStore!!.containsAlias(Constant.KEY_ALIAS)) {
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Constant.KEYSTORE)

                generator.init(KeyGenParameterSpec.Builder(Constant.KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .build()
                )

                generator.generateKey()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    @SuppressLint("InlinedApi")
    fun getCipher(): Boolean {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7)
            return true
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        }

        return false
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun initCipher(mode: Int): Boolean {
        try {
            keyStore!!.load(null)
            val keySpec = keyStore!!.getKey(Constant.KEY_ALIAS, null) as SecretKey

            if (mode == Cipher.ENCRYPT_MODE) {
                cipher!!.init(mode, keySpec)
            } else {
                val iv = PINStorage.loadEncryptedIV()
                val ivSpec = IvParameterSpec(iv)
                cipher!!.init(mode, keySpec, ivSpec)
            }

            return true
        } catch (e: KeyPermanentlyInvalidatedException) {
            e.printStackTrace()
            // retry after clearing entry
            createNewKey(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun initCryptObject(): Boolean {
        try {
            cryptoObject = FingerprintManager.CryptoObject(cipher!!)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    @SuppressLint("SetTextI18n")
    private fun buttonClick(button: Button) {
        tvPin.text = String.format("%s%s", tvPin.text, button.text)
    }

    private fun doSavePIN() {
        if (confirmWithFingerprintTask != null) {
            return
        }

        tvPin.error = null

        val pin = tvPin.text.toString()

        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(pin)) {
            tvPin.error = getString(R.string.error_empty_pin)
            focusView = tvPin
            cancel = true
        }

        if (cancel) {
            focusView!!.requestFocus()
        } else {
            if (usePIN2) {
                if (!testFingerPrintSettings()) {
                    tvPin.postDelayed({ navigateUp() }, 2000)
                    return
                }

                onConfirmAction = OnConfirmAction.Save

                confirmWithFingerprintTask = ConfirmWithFingerprintTask(this@PinSaveFragment)
                confirmWithFingerprintTask!!.execute(null as Void?)

            } else {
                if (cbUseFingerprint.isChecked || PINStorage.haveEncryptedPIN()) {
                    if (!testFingerPrintSettings()) {
                        tvPin.postDelayed({ navigateUp() }, 2000)
                        return
                    }

                    onConfirmAction = if (cbUseFingerprint.isChecked) {
                        OnConfirmAction.Save
                    } else {
                        OnConfirmAction.DeleteEncryptedAndSave
                    }

                    // show a progress spinner, and kick off a background task to perform the user login attempt
                    confirmWithFingerprintTask = ConfirmWithFingerprintTask(this@PinSaveFragment)
                    confirmWithFingerprintTask!!.execute(null as Void?)
                } else {
                    PINStorage.savePIN(tvPin.text.toString())
                    navigateUp()
                }
            }
        }
    }

    private fun doDeletePIN() {
        if (usePIN2) {
            if (PINStorage.haveEncryptedPIN2()) {
                if (!testFingerPrintSettings()) {
                    tvPin.postDelayed({ navigateUp() }, 2000)
                    return
                }
                onConfirmAction = OnConfirmAction.Delete
                confirmWithFingerprintTask = ConfirmWithFingerprintTask(this@PinSaveFragment)
                confirmWithFingerprintTask!!.execute(null as Void?)
            } else {
                tvPin.text = ""
                navigateUp()
            }
        } else {
            if (cbUseFingerprint.isChecked || PINStorage.haveEncryptedPIN()) {
                if (!testFingerPrintSettings()) {
                    tvPin.postDelayed({ navigateUp() }, 2000)
                    return
                }
                onConfirmAction = OnConfirmAction.Delete
                confirmWithFingerprintTask = ConfirmWithFingerprintTask(this@PinSaveFragment)
                confirmWithFingerprintTask!!.execute(null as Void?)
            } else {
                tvPin.text = ""
                PINStorage.deletePIN()
                navigateUp()
            }
        }
    }

    @SuppressLint("NewApi")
    private fun testFingerPrintSettings(): Boolean {
        val keyguardManager = activity?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        fingerprintManager = activity?.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

        if (!keyguardManager.isKeyguardSecure) {
            Toast.makeText(context, R.string.user_has_not_enabled_lock_screen, Toast.LENGTH_LONG).show()
            return false
        }

        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, R.string.user_has_not_granted_permission_to_use_fingerprint, Toast.LENGTH_LONG).show()
            return false
        }

        if (!fingerprintManager!!.hasEnrolledFingerprints()) {
            Toast.makeText(context, R.string.user_has_not_registered_any_fingerprints, Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

}