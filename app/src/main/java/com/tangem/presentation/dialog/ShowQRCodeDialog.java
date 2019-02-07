package com.tangem.presentation.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.tangem.util.UtilHelper;
import com.tangem.wallet.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

/**
 * Created by dvol on 06.03.2018.
 */
public class ShowQRCodeDialog extends DialogFragment {
    private ImageView ivQR;
    private TextView tvQRaddress;
    private Bitmap bmQR;
    private String addr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View v = inflater.inflate(R.layout.dialog_show_qr, null);
        ivQR = v.findViewById(R.id.ivQR);
        ivQR.setImageBitmap(bmQR);
        tvQRaddress = v.findViewById(R.id.tvQRaddress);
        tvQRaddress.setText(addr);

        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle(R.string.show_wallet_qr_code)
                .setView(v)
                .setPositiveButton(R.string.ok, (dialog,which)->dismiss() )
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    public void setup(String content) {
        try {
            bmQR = UtilHelper.INSTANCE.generateQrCode(content);
            addr = content;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void show(final AppCompatActivity activity, final String content) {
        activity.runOnUiThread(() -> {
                    ShowQRCodeDialog instance = new ShowQRCodeDialog();
                    instance.setup(content);
                    instance.show(activity.getSupportFragmentManager(), "ShowQRCodeDialog");
                });
    }

}