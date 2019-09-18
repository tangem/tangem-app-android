package com.tangem.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.tangem.Constant;
import com.tangem.wallet.R;

public class PINSwapWarningDialog extends DialogFragment {
    public static final String TAG = PINSwapWarningDialog.class.getSimpleName();

    private String message = "";
    private OnPositiveButton mOnPositiveButton;

    public interface OnPositiveButton {
        void onRefresh();
    }

    public void setOnRefreshPage(OnPositiveButton listener) {
        mOnPositiveButton = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            message = getArguments().getString(Constant.EXTRA_MESSAGE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle(R.string.dialog_title_money_is_at_risk)
                .setMessage(message)
                .setCancelable(true)
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> dismiss())
                .setPositiveButton(R.string.general_continue, (dialog, whichButton) -> mOnPositiveButton.onRefresh())
                .create();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
    }

}