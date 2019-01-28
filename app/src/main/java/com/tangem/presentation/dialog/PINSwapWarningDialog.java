package com.tangem.presentation.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;

import com.tangem.wallet.R;

public class PINSwapWarningDialog extends DialogFragment {
    public static final String TAG = PINSwapWarningDialog.class.getSimpleName();

    public static final String EXTRA_MESSAGE = "message";
    private String message;
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
        message = getArguments().getString(EXTRA_MESSAGE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle(R.string.your_money_is_at_risk)
                .setMessage(message)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dismiss())
                .setPositiveButton(R.string.contin, (dialog, whichButton) -> mOnPositiveButton.onRefresh())
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

}