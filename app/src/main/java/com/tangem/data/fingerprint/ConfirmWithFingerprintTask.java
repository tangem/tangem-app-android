package com.tangem.data.fingerprint;

import android.os.AsyncTask;
import android.widget.Toast;

import com.tangem.ui.fragment.pin.PinSaveFragment;
import com.tangem.wallet.R;

import java.lang.ref.WeakReference;

import javax.crypto.Cipher;

public class ConfirmWithFingerprintTask extends AsyncTask<Void, Void, Boolean> {
    private WeakReference<PinSaveFragment> reference;

    public ConfirmWithFingerprintTask(PinSaveFragment context) {
        reference = new WeakReference<>(context);

        reference.get().setFingerprintHelper(new FingerprintHelper(reference.get()));
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        PinSaveFragment pinSaveFragment = reference.get();

        if (!pinSaveFragment.getKeyStore())
            return false;

        if (!pinSaveFragment.createNewKey(false))
            return false;

        if (!pinSaveFragment.getCipher())
            return false;

        return pinSaveFragment.initCipher(Cipher.ENCRYPT_MODE) && pinSaveFragment.initCryptObject();

    }

    @Override
    protected void onPostExecute(final Boolean success) {
        PinSaveFragment pinSaveFragment = reference.get();

        onCancelled();

        if (!success) {
            Toast.makeText(pinSaveFragment.getContext(), R.string.pin_save_fail, Toast.LENGTH_LONG).show();
        } else {
            pinSaveFragment.getFingerprintHelper().startAuth(pinSaveFragment.getFingerprintManager(), pinSaveFragment.getCryptoObject());
        }
    }

    @Override
    protected void onCancelled() {
        PinSaveFragment pinSaveFragment = reference.get();

        if (pinSaveFragment.getDFingerPrintConfirmation() != null) {
            pinSaveFragment.getDFingerPrintConfirmation().cancel();
        }
    }

}