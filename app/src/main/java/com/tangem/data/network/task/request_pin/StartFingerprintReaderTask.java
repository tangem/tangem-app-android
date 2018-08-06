package com.tangem.data.network.task.request_pin;

import android.annotation.TargetApi;
import android.hardware.fingerprint.FingerprintManager;
import android.os.AsyncTask;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import com.tangem.domain.wallet.FingerprintHelper;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.presentation.activity.RequestPINActivity;

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
    private WeakReference<RequestPINActivity> reference;

    private KeyStore keyStore;
    private Cipher cipher;

    private FingerprintManager.CryptoObject cryptoObject;

    FingerprintManager fingerprintManager;
    FingerprintHelper fingerprintHelper;

    RequestPINActivity activity;

    public StartFingerprintReaderTask(RequestPINActivity activity, FingerprintManager fingerprintManager, FingerprintHelper fingerprintHelper) {
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
        RequestPINActivity requestPINActivity = reference.get();

        onCancelled();

        if (!success) {
            requestPINActivity.doLog("Authentication failed!");
        } else {
            fingerprintHelper.startAuth(fingerprintManager, cryptoObject);
            requestPINActivity.doLog("Authenticate using fingerprint!");

        }
    }

    @Override
    protected void onCancelled() {
        RequestPINActivity requestPINActivity = reference.get();

        requestPINActivity.setStartFingerprintReaderTask(null);
    }

    private boolean getKeyStore() {
        RequestPINActivity requestPINActivity = reference.get();

        requestPINActivity.doLog("Getting keystore...");
        try {
            keyStore = KeyStore.getInstance(RequestPINActivity.KEYSTORE);
            keyStore.load(null); // Create empty keystore
            return true;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean createNewKey(boolean forceCreate) {
        RequestPINActivity requestPINActivity = reference.get();

        requestPINActivity.doLog("Creating new key...");
        try {
            if (forceCreate)
                keyStore.deleteEntry(RequestPINActivity.KEY_ALIAS);

            if (!keyStore.containsAlias(RequestPINActivity.KEY_ALIAS)) {
                KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, RequestPINActivity.KEYSTORE);

                generator.init(new KeyGenParameterSpec.Builder(RequestPINActivity.KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .build()
                );

                generator.generateKey();
                requestPINActivity.doLog("Key created.");
            } else
                requestPINActivity.doLog("Key exists.");

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean getCipher() {
        RequestPINActivity requestPINActivity = reference.get();

        requestPINActivity.doLog("Getting cipher...");
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
        RequestPINActivity requestPINActivity = reference.get();

        requestPINActivity.doLog("Initializing cipher...");
        try {
            keyStore.load(null);
            SecretKey keyspec = (SecretKey) keyStore.getKey(RequestPINActivity.KEY_ALIAS, null);

            if (mode == Cipher.ENCRYPT_MODE) {
                cipher.init(mode, keyspec);
            } else {
                byte[] iv = null;
                if (requestPINActivity.getMode() == RequestPINActivity.Mode.RequestPIN || activity.getMode() == RequestPINActivity.Mode.RequestNewPIN || activity.getMode() == RequestPINActivity.Mode.ConfirmNewPIN) {
                    iv = PINStorage.loadEncryptedIV();
                } else if (activity.getMode() == RequestPINActivity.Mode.RequestPIN2 || activity.getMode() == RequestPINActivity.Mode.RequestNewPIN2 || activity.getMode() == RequestPINActivity.Mode.ConfirmNewPIN2) {
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
        RequestPINActivity requestPINActivity = reference.get();

        requestPINActivity.doLog("Initializing crypt object...");
        try {
            cryptoObject = new FingerprintManager.CryptoObject(cipher);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


}
