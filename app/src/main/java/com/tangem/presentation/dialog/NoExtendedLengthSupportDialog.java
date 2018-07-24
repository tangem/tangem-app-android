package com.tangem.presentation.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.tangem.wallet.R;

public class NoExtendedLengthSupportDialog extends DialogFragment {
    public static final String TAG = NoExtendedLengthSupportDialog.class.getSimpleName();

    public static boolean allReadyShowed = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle(R.string.warning)
                .setMessage(R.string.the_nfc_adapter_length_apdu)
                .setPositiveButton(R.string.got_it, (dialog, whichButton) -> NoExtendedLengthSupportDialog.allReadyShowed = true)
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

}