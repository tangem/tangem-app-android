package com.tangem.presentation.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.tangem.wallet.R;

public class NoExtendedLengthSupportDialog extends DialogFragment {

    public static boolean allreadyShowed=false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle("Warning")
                .setMessage("The NFC adapter of the device does not support extended length APDU, it's possible that some functions will not work!")
                .setPositiveButton("Got it",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                NoExtendedLengthSupportDialog.allreadyShowed=true;
                            }
                        }
                )
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }
}
