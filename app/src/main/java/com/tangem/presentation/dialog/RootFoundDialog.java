package com.tangem.presentation.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import com.tangem.wallet.R;

public class RootFoundDialog extends DialogFragment {
    public static final String TAG = RootFoundDialog.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle(R.string.device_is_rooted)
                .setCancelable(false)
                .setPositiveButton(R.string.got_it, null)
                .create();
    }

}