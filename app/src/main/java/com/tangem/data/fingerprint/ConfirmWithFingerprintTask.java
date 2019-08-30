package com.tangem.data.fingerprint;

import android.os.AsyncTask;
import android.widget.Toast;

import com.tangem.ui.activity.PinSaveActivity;
import com.tangem.wallet.R;

import java.lang.ref.WeakReference;

import javax.crypto.Cipher;

public class ConfirmWithFingerprintTask extends AsyncTask<Void, Void, Boolean> {
    private WeakReference<PinSaveActivity> reference;

    public ConfirmWithFingerprintTask(PinSaveActivity context) {
        reference = new WeakReference<>(context);

        reference.get().setFingerprintHelper(new FingerprintHelper(reference.get()));
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        PinSaveActivity pinSaveActivity = reference.get();

        if (!pinSaveActivity.getKeyStore())
            return false;

        if (!pinSaveActivity.createNewKey(false))
            return false;

        if (!pinSaveActivity.getCipher())
            return false;

        return pinSaveActivity.initCipher(Cipher.ENCRYPT_MODE) && pinSaveActivity.initCryptObject();

    }

    @Override
    protected void onPostExecute(final Boolean success) {
        PinSaveActivity pinSaveActivity = reference.get();

        onCancelled();

        if (!success) {
            Toast.makeText(pinSaveActivity, R.string.pin_save_notification_failed, Toast.LENGTH_LONG).show();
        } else {
            pinSaveActivity.getFingerprintHelper().startAuth(pinSaveActivity.getFingerprintManager(), pinSaveActivity.getCryptoObject());
        }
    }

    @Override
    protected void onCancelled() {
        PinSaveActivity pinSaveActivity = reference.get();

        if (pinSaveActivity.getDFingerPrintConfirmation() != null) {
            pinSaveActivity.getDFingerPrintConfirmation().cancel();
        }
    }

}