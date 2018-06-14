package com.tangem.presentation.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.tangem.domain.wallet.FingerprintHelper;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.wallet.R;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

//import com.afollestad.materialdialogs.AlertDialogWrapper;

public class SavePINActivity extends AppCompatActivity implements FingerprintHelper.FingerprintHelperListener {

    private TextView tvPIN;
    private CheckBox chkUseFingerprint;

    private ConfirmWithFingerprintTask mConfirmWithFingerprintTask;


    private KeyStore keyStore;
    private Cipher cipher;
    private FingerprintManager fingerprintManager;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintHelper fingerprintHelper;

    private boolean UsePIN2 = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_pin);

        MainActivity.commonInit(getApplicationContext());

        UsePIN2 = getIntent().getBooleanExtra("PIN2", false);

        tvPIN = findViewById(R.id.pin);
        OnClickListener onButtonNClick = view -> tvPIN.setText(String.format("%s%s", tvPIN.getText(), ((Button) view).getText()));

        if (UsePIN2) {
            ((TextView) findViewById(R.id.pin_prompt)).setText(R.string.enter_pin2_and_use_fingerprint_to_save_it);
        } else {
            ((TextView) findViewById(R.id.pin_prompt)).setText(R.string.enter_pin_and_use_fingerprint_to_save_it);
        }

        chkUseFingerprint = findViewById(R.id.chkUseFingerprint);

        if (UsePIN2) {
            chkUseFingerprint.setChecked(true);
            chkUseFingerprint.setEnabled(false);
        } else {
            chkUseFingerprint.setChecked(PINStorage.haveEncryptedPIN());
            chkUseFingerprint.setEnabled(true);
        }


        Button btn0 = findViewById(R.id.btn0);
        btn0.setOnClickListener(onButtonNClick);
        Button btn1 = findViewById(R.id.btn1);
        btn1.setOnClickListener(onButtonNClick);
        Button btn2 = findViewById(R.id.btn2);
        btn2.setOnClickListener(onButtonNClick);
        Button btn3 = findViewById(R.id.btn3);
        btn3.setOnClickListener(onButtonNClick);
        Button btn4 = findViewById(R.id.btn4);
        btn4.setOnClickListener(onButtonNClick);
        Button btn5 = findViewById(R.id.btn5);
        btn5.setOnClickListener(onButtonNClick);
        Button btn6 = findViewById(R.id.btn6);
        btn6.setOnClickListener(onButtonNClick);
        Button btn7 = findViewById(R.id.btn7);
        btn7.setOnClickListener(onButtonNClick);
        Button btn8 = findViewById(R.id.btn8);
        btn8.setOnClickListener(onButtonNClick);
        Button btn9 = findViewById(R.id.btn9);
        btn9.setOnClickListener(onButtonNClick);
        Button btnBS = findViewById(R.id.btnBackspace);

        btnBS.setOnClickListener(view -> {
            String S = tvPIN.getText().toString();
            if (S.length() > 0) {
                tvPIN.setText(S.substring(0, S.length() - 1));
            }
        });

        Button btnSave = findViewById(R.id.btnSavePIN);
        btnSave.setOnClickListener(view -> doSavePIN());

        Button btnDelete = findViewById(R.id.btnDeletePIN);
        btnDelete.setOnClickListener(view -> doDeletePIN());

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (fingerprintHelper != null)
            fingerprintHelper.cancel();

        if (mConfirmWithFingerprintTask != null)
            mConfirmWithFingerprintTask.cancel(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (fingerprintHelper != null)
            fingerprintHelper.cancel();

        if (mConfirmWithFingerprintTask != null)
            mConfirmWithFingerprintTask.cancel(true);
    }


    private enum OnConfirmAction {Save, DeleteEncryptedAndSave, Delete}

    OnConfirmAction onConfirmAction;

    private void doSavePIN() {
        if (mConfirmWithFingerprintTask != null) {
            return;
        }

        tvPIN.setError(null);

        String pin = tvPIN.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(pin)) {
            tvPIN.setError(getString(R.string.error_empty_pin));
            focusView = tvPIN;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {

            if (UsePIN2) {
                if (!testFingerPrintSettings()) {
                    tvPIN.postDelayed(this::finish, 2000);
                    return;
                }

                onConfirmAction = OnConfirmAction.Save;

                mConfirmWithFingerprintTask = new ConfirmWithFingerprintTask();
                mConfirmWithFingerprintTask.execute((Void) null);
            } else {
                if (chkUseFingerprint.isChecked() || PINStorage.haveEncryptedPIN()) {
                    if (!testFingerPrintSettings()) {
                        tvPIN.postDelayed(this::finish, 2000);
                        return;
                    }

                    if (chkUseFingerprint.isChecked()) {
                        onConfirmAction = OnConfirmAction.Save;
                    } else {
                        onConfirmAction = OnConfirmAction.DeleteEncryptedAndSave;
                    }

                    // Show a progress spinner, and kick off a background task to
                    // perform the user login attempt.
                    //showProgress(true);
                    mConfirmWithFingerprintTask = new ConfirmWithFingerprintTask();
                    mConfirmWithFingerprintTask.execute((Void) null);
                } else {
                    PINStorage.savePIN(tvPIN.getText().toString());
                    finish();
                }
            }
        }
    }

    private void doDeletePIN() {
        if (UsePIN2) {
            if (PINStorage.haveEncryptedPIN2()) {
                if (!testFingerPrintSettings()) {
                    tvPIN.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 2000);
                    return;
                }
                onConfirmAction = OnConfirmAction.Delete;
                mConfirmWithFingerprintTask = new ConfirmWithFingerprintTask();
                mConfirmWithFingerprintTask.execute((Void) null);
            } else {
                tvPIN.setText("");
                finish();
            }
        } else {
            if (chkUseFingerprint.isChecked() || PINStorage.haveEncryptedPIN()) {
                if (!testFingerPrintSettings()) {
                    tvPIN.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 2000);
                    return;
                }
                onConfirmAction = OnConfirmAction.Delete;
                mConfirmWithFingerprintTask = new ConfirmWithFingerprintTask();
                mConfirmWithFingerprintTask.execute((Void) null);
            } else {
                tvPIN.setText("");
                PINStorage.deletePIN();
                finish();
            }
        }
    }


    @Override
    public void authenticationFailed(String error) {
        print(error);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void authenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        print("Authentication succeeded!");
        cipher = result.getCryptoObject().getCipher();

        switch (onConfirmAction) {
            case Save:
                String textToEncrypt = tvPIN.getText().toString();
                if (UsePIN2) {
                    PINStorage.saveEncryptedPIN2(cipher, textToEncrypt);
                } else {
                    PINStorage.saveEncryptedPIN(cipher, textToEncrypt);
                }
                print(R.string.pin_save_success);
                break;
            case Delete:
                if (UsePIN2) {
                    PINStorage.deleteEncryptedPIN2();
                } else {
                    PINStorage.deleteEncryptedPIN();
                    PINStorage.deletePIN();
                }
                tvPIN.setText("");
                break;
            case DeleteEncryptedAndSave:
                if (UsePIN2) {
                    PINStorage.deleteEncryptedPIN2();
                    PINStorage.saveEncryptedPIN2(cipher, tvPIN.getText().toString());
                } else {
                    PINStorage.deleteEncryptedPIN();
                    PINStorage.savePIN(tvPIN.getText().toString());
                }
                break;
        }
        dFingerPrintConfirmation.dismiss();
        finish();
    }

    private class ConfirmWithFingerprintTask extends AsyncTask<Void, Void, Boolean> {
        ConfirmWithFingerprintTask() {
            fingerprintHelper = new FingerprintHelper(SavePINActivity.this);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (!getKeyStore())
                return false;

            if (!createNewKey(false))
                return false;

            if (!getCipher())
                return false;

            return initCipher(Cipher.ENCRYPT_MODE) && initCryptObject();

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            onCancelled();

            if (!success) {
                Toast.makeText(getBaseContext(), R.string.pin_save_fail, Toast.LENGTH_LONG).show();
            } else {
                print("Confirm PIN action using fingerprint!");
                fingerprintHelper.startAuth(fingerprintManager, cryptoObject);
                CreateFingerPrintConfirmationDialog();
            }
        }

        @Override
        protected void onCancelled() {
            mConfirmWithFingerprintTask = null;
            if (dFingerPrintConfirmation != null) {
                dFingerPrintConfirmation.cancel();
            }
        }
    }

    public void print(String text) {
//        Log.e("FP", text);
    }

    public void print(int id) {
        print(getString(id));
    }

    @SuppressLint("NewApi")
    private boolean testFingerPrintSettings() {
        print("Testing Fingerprint Settings");

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        if (!keyguardManager.isKeyguardSecure()) {
            print("User hasn't enabled Lock Screen");
            Toast.makeText(getBaseContext(), "User hasn't enabled Lock Screen", Toast.LENGTH_LONG).show();
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            print("User hasn't granted permission to use Fingerprint");
            Toast.makeText(getBaseContext(), "User hasn't granted permission to use Fingerprint", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!fingerprintManager.hasEnrolledFingerprints()) {
            print("User hasn't registered any fingerprints");
            Toast.makeText(getBaseContext(), "User hasn't registered any fingerprints", Toast.LENGTH_LONG).show();
            return false;
        }

        print("Fingerprint authentication is set.\n");

        return true;
    }

    private boolean getKeyStore() {
        print("Getting keystore...");
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
        print("Creating new key...");
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
                print("Key created.");
            } else
                print("Key exists.");

            return true;
        } catch (Exception e) {
            print(e.getMessage());
        }

        return false;
    }

    private boolean getCipher() {
        print("Getting cipher...");
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean initCipher(int mode) {
        print("Initializing cipher...");
        try {
            keyStore.load(null);
            SecretKey keyspec = (SecretKey) keyStore.getKey(RequestPINActivity.KEY_ALIAS, null);

            if (mode == Cipher.ENCRYPT_MODE) {
                cipher.init(mode, keyspec);
            } else {
                byte[] iv = PINStorage.loadEncryptedIV();
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
        print("Initializing crypt object...");
        try {
            cryptoObject = new FingerprintManager.CryptoObject(cipher);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    Dialog dFingerPrintConfirmation = null;

    private void CreateFingerPrintConfirmationDialog() {
//        final AlertDialogWrapper.Builder b = new AlertDialogWrapper.Builder(this);
//        switch (onConfirmAction)
//        {
//            case Save:
//                if( UsePIN2) {
//                    b.setTitle("Confirm new PIN2 saving ");
//                }else{
//                    b.setTitle("Confirm new PIN saving ");
//                }
//                break;
//            case Delete:
//                if( UsePIN2 ) {
//                    b.setTitle("Confirm PIN2 deleting");
//                }else{
//                    b.setTitle("Confirm PIN deleting");
//                }
//                break;
//            case DeleteEncryptedAndSave:
//                if( UsePIN2 ) {
//                    b.setTitle("Confirm deleting old saved PIN2");
//                }else {
//                    b.setTitle("Confirm deleting old saved PIN");
//                }
//                break;
//        }
//        View view = getLayoutInflater().inflate(R.layout.dialog_fingerprint_confirmation, null);
//        b.setView(view);

//        dFingerPrintConfirmation = b.show();
//        dFingerPrintConfirmation.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                fingerprintHelper.cancel();
//                print("Cancel fingerprint confirmation");
//            }
//        });
    }
}

