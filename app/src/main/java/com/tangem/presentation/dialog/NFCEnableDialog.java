package com.tangem.presentation.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

import com.tangem.wallet.R;

/**
 * Created by dvol on 18.02.2018.
 */

public class NFCEnableDialog extends DialogFragment {
    public static final String TAG = NFCEnableDialog.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false)
                .setIcon(R.drawable.ic_action_nfc_gray)
                .setTitle(R.string.nfc_disabled)
                .setMessage(R.string.enable_nfc)
                .setPositiveButton(R.string.dialog_ok,
                        (dialog, id) -> {
                            // take user to wireless settings
                            getActivity().startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                        })
                .setNegativeButton(R.string.dialog_quit,
                        (dialog, id) -> {
                            dialog.cancel();
                            getActivity().finish();
                        });
        return builder.create();
    }

}
