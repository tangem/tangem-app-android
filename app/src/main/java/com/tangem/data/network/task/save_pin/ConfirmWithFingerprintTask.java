package com.tangem.data.network.task.save_pin;

import android.os.AsyncTask;
import android.widget.Toast;

import com.tangem.domain.wallet.FingerprintHelper;
import com.tangem.presentation.activity.SavePINActivity;
import com.tangem.wallet.R;

import java.lang.ref.WeakReference;

import javax.crypto.Cipher;

public class ConfirmWithFingerprintTask extends AsyncTask<Void, Void, Boolean> {
    private WeakReference<SavePINActivity> reference;

    public ConfirmWithFingerprintTask(SavePINActivity context) {
        reference = new WeakReference<>(context);

        reference.get().setFingerprintHelper(new FingerprintHelper(reference.get()));
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        SavePINActivity savePINActivity = reference.get();

        if (!savePINActivity.getKeyStore())
            return false;

        if (!savePINActivity.createNewKey(false))
            return false;

        if (!savePINActivity.getCipher())
            return false;

        return savePINActivity.initCipher(Cipher.ENCRYPT_MODE) && savePINActivity.initCryptObject();

    }

    @Override
    protected void onPostExecute(final Boolean success) {
        SavePINActivity savePINActivity = reference.get();

        onCancelled();

        if (!success) {
            Toast.makeText(savePINActivity, R.string.pin_save_fail, Toast.LENGTH_LONG).show();
        } else {
            savePINActivity.getFingerprintHelper().startAuth(savePINActivity.getFingerprintManager(), savePINActivity.getCryptoObject());
        }
    }

    @Override
    protected void onCancelled() {
        SavePINActivity savePINActivity = reference.get();

        if (savePINActivity.getDFingerPrintConfirmation() != null) {
            savePINActivity.getDFingerPrintConfirmation().cancel();
        }
    }

}