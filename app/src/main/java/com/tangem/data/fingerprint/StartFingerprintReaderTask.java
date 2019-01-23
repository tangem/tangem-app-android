package com.tangem.data.fingerprint;

import android.annotation.TargetApi;
import android.hardware.fingerprint.FingerprintManager;
import android.os.AsyncTask;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import com.tangem.Constant;
import com.tangem.tangemcard.android.data.PINStorage;
import com.tangem.presentation.activity.PinRequestActivity;
import com.tangem.util.LOG;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class StartFingerprintReaderTask extends AsyncTask<Void, Void, Boolean> {
    public static final String TAG = StartFingerprintReaderTask.class.getSimpleName();

    private WeakReference<PinRequestActivity> reference;

    private KeyStore keyStore;
    private Cipher cipher;

    private FingerprintManager.CryptoObject cryptoObject;

    FingerprintManager fingerprintManager;
    FingerprintHelper fingerprintHelper;

    PinRequestActivity activity;

    public StartFingerprintReaderTask(PinRequestActivity activity, FingerprintManager fingerprintManager, FingerprintHelper fingerprintHelper) {
        reference = new WeakReference<>(activity);
        this.fingerprintManager = fingerprintManager;
        this.fingerprintHelper = fingerprintHelper;
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (!getKeyStore())
            return false;

        if (!createNewKey(false))
            return false;

        if (!getCipher())
            return false;

        if (!initCipher(Cipher.DECRYPT_MODE))
            return false;

        return initCryptObject();
    }

    @Override
    protected void onPostExecute(final Boolean success) {

        onCancelled();

        if (!success) {
            LOG.i(TAG, "Authentication failed!");
        } else {
            fingerprintHelper.startAuth(fingerprintManager, cryptoObject);
            LOG.i(TAG, "Authenticate using fingerprint!");
        }
    }

    @Override
    protected void onCancelled() {
        PinRequestActivity pinRequestActivity = reference.get();

        pinRequestActivity.setStartFingerprintReaderTask(null);
    }

    private boolean getKeyStore() {
        LOG.i(TAG, "Getting keystore...");
        try {
            keyStore = KeyStore.getInstance(Constant.KEYSTORE);
            keyStore.load(null); // Create empty keystore
            return true;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean createNewKey(boolean forceCreate) {
        LOG.i(TAG, "Creating new key...");
        try {
            if (forceCreate)
                keyStore.deleteEntry(Constant.KEY_ALIAS);

            if (!keyStore.containsAlias(Constant.KEY_ALIAS)) {
                KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Constant.KEYSTORE);

                generator.init(new KeyGenParameterSpec.Builder(Constant.KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .build()
                );

                generator.generateKey();
                LOG.i(TAG, "Key created.");
            } else
                LOG.i(TAG, "Key exists.");

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean getCipher() {
        LOG.i(TAG, "Getting cipher...");
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean initCipher(int mode) {
        PinRequestActivity pinRequestActivity = reference.get();

        LOG.i(TAG, "Initializing cipher...");
        try {
            keyStore.load(null);
            SecretKey keyspec = (SecretKey) keyStore.getKey(Constant.KEY_ALIAS, null);

            if (mode == Cipher.ENCRYPT_MODE) {
                cipher.init(mode, keyspec);
            } else {
                byte[] iv = null;
                if (pinRequestActivity.getMode() == PinRequestActivity.Mode.RequestPIN || activity.getMode() == PinRequestActivity.Mode.RequestNewPIN || activity.getMode() == PinRequestActivity.Mode.ConfirmNewPIN) {
                    iv = PINStorage.loadEncryptedIV();
                } else if (activity.getMode() == PinRequestActivity.Mode.RequestPIN2 || activity.getMode() == PinRequestActivity.Mode.RequestNewPIN2 || activity.getMode() == PinRequestActivity.Mode.ConfirmNewPIN2) {
                    iv = PINStorage.loadEncryptedIV2();
                }
                IvParameterSpec ivspec = new IvParameterSpec(iv);
                cipher.init(mode, keyspec, ivspec);
            }

            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            e.printStackTrace();
            createNewKey(true); // Retry after clearing entry
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean initCryptObject() {
        LOG.i(TAG, "Initializing crypt object...");
        try {
            cryptoObject = new FingerprintManager.CryptoObject(cipher);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
