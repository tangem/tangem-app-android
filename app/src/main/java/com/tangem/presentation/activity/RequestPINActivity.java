package com.tangem.presentation.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.FingerprintHelper;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.domain.wallet.TangemCard;
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

public class RequestPINActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, FingerprintHelper.FingerprintHelperListener {

    public enum Mode {RequestPIN, RequestPIN2, RequestNewPIN, RequestNewPIN2, ConfirmNewPIN, ConfirmNewPIN2}

    Mode mode;
    boolean allowFingerprint = false;

    private NfcManager mNfcManager;

    private TextView tvPIN;

    private StartFingerprintReaderTask mStartFingerprintReaderTask;

    public static final String KEY_ALIAS = "pinKey";
    public static final String KEYSTORE = "AndroidKeyStore";

    private FingerprintManager fingerprintManager;
    private FingerprintHelper fingerprintHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_pin);

        MainActivity.commonInit(getApplicationContext());

        mNfcManager = new NfcManager(this, this);

        tvPIN = findViewById(R.id.pin);

        OnClickListener onButtonNClick = new OnClickListener() {
            @Override
            public void onClick(View view) {
                tvPIN.setText(tvPIN.getText() + (String) ((Button) view).getText());
            }
        };

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
        btnBS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String S = tvPIN.getText().toString();
                if (S.length() > 0) {
                    tvPIN.setText(S.substring(0, S.length() - 1));
                }
            }
        });

        Button btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                doContinue();
            }
        });

        mode = Mode.valueOf(getIntent().getStringExtra("mode"));
        TextView tvPrompt = findViewById(R.id.pin_prompt);
        if (mode == Mode.RequestNewPIN) {
            if (PINStorage.haveEncryptedPIN()) {
                allowFingerprint = true;
                tvPrompt.setText("Enter new PIN or use fingerprint scanner");
            } else {
                tvPrompt.setText("Enter new PIN");
            }
        } else if (mode == Mode.ConfirmNewPIN) {
            tvPrompt.setText("Confirm new PIN");
        } else if (mode == Mode.RequestPIN) {
            if (PINStorage.haveEncryptedPIN()) {
                allowFingerprint = true;
                tvPrompt.setText("Enter PIN or use fingerprint scanner");
            } else {
                tvPrompt.setText("Enter PIN");
            }
        } else if (mode == Mode.RequestNewPIN2) {
            if (PINStorage.haveEncryptedPIN2()) {
                allowFingerprint = true;
                tvPrompt.setText("Enter new PIN2 or use fingerprint scanner");
            } else {
                tvPrompt.setText("Enter new PIN2");
            }
        } else if (mode == Mode.ConfirmNewPIN2) {
            tvPrompt.setText("Confirm new PIN2");
        } else if (mode == Mode.RequestPIN2) {
            String UID = getIntent().getStringExtra("UID");
            TangemCard mCard = new TangemCard(UID);
            mCard.LoadFromBundle(getIntent().getBundleExtra("Card"));

            if (mCard.PIN2 == TangemCard.PIN2_Mode.DefaultPIN2 || mCard.PIN2 == TangemCard.PIN2_Mode.Unchecked) {
                // if we know PIN2 or not try default previously - use it
                PINStorage.setPIN2(PINStorage.getDefaultPIN2());
                setResult(Activity.RESULT_OK);
                finish();
                return;
            }

            if (PINStorage.haveEncryptedPIN2()) {
                allowFingerprint = true;
                tvPrompt.setText("Enter PIN2 or use fingerprint scanner");
            } else {
                tvPrompt.setText("Enter PIN2");
            }
        }
        ImageView imgFingerprint = findViewById(R.id.imgFingerprint);
        if (!allowFingerprint) {
            imgFingerprint.setVisibility(View.GONE);
        } else {
            imgFingerprint.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (fingerprintHelper != null)
            fingerprintHelper.cancel();

        if (mStartFingerprintReaderTask != null) {
            mStartFingerprintReaderTask.cancel(true);
            mStartFingerprintReaderTask = null;
        }

        mNfcManager.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (fingerprintHelper != null)
            fingerprintHelper.cancel();

        if (mStartFingerprintReaderTask != null) {

            mStartFingerprintReaderTask.cancel(true);
            mStartFingerprintReaderTask = null;
        }

        mNfcManager.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcManager.onResume();
        if (allowFingerprint) {
            startFingerprintReader();
        }
    }

    private void doContinue() {
        if (mStartFingerprintReaderTask != null) {
            return;
        }

        // Reset errors.
        tvPIN.setError(null);

        // Store values at the time of the login attempt.
        String pin = tvPIN.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (mode == Mode.ConfirmNewPIN) {
            if (!pin.equals(getIntent().getStringExtra("newPIN"))) {
                tvPIN.setError(getString(R.string.error_pin_confirmation_failed));
                focusView = tvPIN;
                cancel = true;
            }
        } else if (mode == Mode.ConfirmNewPIN2) {
            if (!pin.equals(getIntent().getStringExtra("newPIN2"))) {
                tvPIN.setError(getString(R.string.error_pin_confirmation_failed));
                focusView = tvPIN;
                cancel = true;
            }
        } else {
            if (TextUtils.isEmpty(pin)) {
                tvPIN.setError(getString(R.string.error_empty_pin));
                focusView = tvPIN;
                cancel = true;
            }
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            if (mode == Mode.RequestNewPIN || mode == Mode.ConfirmNewPIN) {
                Intent resultData = new Intent();
                resultData.putExtra("newPIN", pin);
                if (mode == Mode.ConfirmNewPIN) {
                    resultData.putExtra("confirmPIN", pin);
                }
                setResult(Activity.RESULT_OK, resultData);
                finish();
            } else if (mode == Mode.RequestNewPIN2 || mode == Mode.ConfirmNewPIN2) {
                Intent resultData = new Intent();
                resultData.putExtra("newPIN2", pin);
                if (mode == Mode.ConfirmNewPIN2) {
                    resultData.putExtra("confirmPIN2", pin);
                }
                setResult(Activity.RESULT_OK, resultData);
                finish();
            } else if (mode == Mode.RequestPIN) {
                PINStorage.setUserPIN(pin);
                setResult(Activity.RESULT_OK);
                finish();
            } else if (mode == Mode.RequestPIN2) {
                PINStorage.setPIN2(pin);
                setResult(Activity.RESULT_OK);
                finish();
            }
        }

    }

    @Override
    public void authenticationFailed(String error) {
        doLog(error);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void authenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        doLog("Authentication succeeded!");
        Cipher cipher = result.getCryptoObject().getCipher();

        if (mode == Mode.RequestNewPIN || mode == Mode.ConfirmNewPIN) {
            Intent resultData = new Intent();
            String pin = PINStorage.loadEncryptedPIN(cipher);
            resultData.putExtra("newPIN", pin);
            resultData.putExtra("confirmPIN", pin);
            setResult(Activity.RESULT_OK, resultData);
            finish();
        } else if (mode == Mode.RequestNewPIN2 || mode == Mode.ConfirmNewPIN2) {
            Intent resultData = new Intent();
            String pin = PINStorage.loadEncryptedPIN2(cipher);
            resultData.putExtra("newPIN2", pin);
            resultData.putExtra("confirmPIN2", pin);
            setResult(Activity.RESULT_OK, resultData);
            finish();
        } else if (mode == Mode.RequestPIN) {
            PINStorage.loadEncryptedPIN(cipher);
            setResult(Activity.RESULT_OK);
        } else if (mode == Mode.RequestPIN2) {
            PINStorage.loadEncryptedPIN2(cipher);
            setResult(Activity.RESULT_OK);
        }

        finish();
    }

    private void startFingerprintReader() {
        if (!testFingerPrintSettings())
            return;

        if (!allowFingerprint)
            return;

        fingerprintHelper = new FingerprintHelper(RequestPINActivity.this);
        mStartFingerprintReaderTask = new StartFingerprintReaderTask(this, fingerprintManager, fingerprintHelper);
        mStartFingerprintReaderTask.execute((Void) null);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            Log.w(getClass().getName(), "Ignore discovered tag!");
            mNfcManager.IgnoreTag(tag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static class StartFingerprintReaderTask extends AsyncTask<Void, Void, Boolean> {
        private KeyStore keyStore;
        private Cipher cipher;

        private FingerprintManager.CryptoObject cryptoObject;

        FingerprintManager fingerprintManager;
        FingerprintHelper fingerprintHelper;

        RequestPINActivity activity;

        StartFingerprintReaderTask(RequestPINActivity activity, FingerprintManager fingerprintManager, FingerprintHelper fingerprintHelper) {
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
                doLog("Authentication failed!");
            } else {
                fingerprintHelper.startAuth(fingerprintManager, cryptoObject);
                doLog("Authenticate using fingerprint!");

            }
        }

        @Override
        protected void onCancelled() {
            activity.mStartFingerprintReaderTask = null;
        }

        private boolean getKeyStore() {
            doLog("Getting keystore...");
            try {
                keyStore = KeyStore.getInstance(KEYSTORE);
                keyStore.load(null); // Create empty keystore
                return true;
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @TargetApi(Build.VERSION_CODES.M)
        public boolean createNewKey(boolean forceCreate) {
            doLog("Creating new key...");
            try {
                if (forceCreate)
                    keyStore.deleteEntry(KEY_ALIAS);

                if (!keyStore.containsAlias(KEY_ALIAS)) {
                    KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);

                    generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                            .setUserAuthenticationRequired(true)
                            .build()
                    );

                    generator.generateKey();
                    doLog("Key created.");
                } else
                    doLog("Key exists.");

                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        private boolean getCipher() {
            doLog("Getting cipher...");
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
            doLog("Initializing cipher...");
            try {
                keyStore.load(null);
                SecretKey keyspec = (SecretKey) keyStore.getKey(KEY_ALIAS, null);

                if (mode == Cipher.ENCRYPT_MODE) {
                    cipher.init(mode, keyspec);
                } else {
                    byte[] iv = null;
                    if (activity.mode == Mode.RequestPIN || activity.mode == Mode.RequestNewPIN || activity.mode == Mode.ConfirmNewPIN) {
                        iv = PINStorage.loadEncryptedIV();
                    } else if (activity.mode == Mode.RequestPIN2 || activity.mode == Mode.RequestNewPIN2 || activity.mode == Mode.ConfirmNewPIN2) {
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
            doLog("Initializing crypt object...");
            try {
                cryptoObject = new FingerprintManager.CryptoObject(cipher);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }


    }

    public static void doLog(String text) {
//        Log.e("FP", text);
    }

    @SuppressLint("NewApi")
    private boolean testFingerPrintSettings() {
        doLog("Testing Fingerprint Settings");

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        assert keyguardManager != null;
        if (!keyguardManager.isKeyguardSecure()) {
            doLog("User hasn't enabled Lock Screen");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            doLog("User hasn't granted permission to use Fingerprint");
            return false;
        }

        if (!fingerprintManager.hasEnrolledFingerprints()) {
            doLog("User hasn't registered any fingerprints");
            return false;
        }

        doLog("Fingerprint authentication is set.\n");

        return true;
    }


}

