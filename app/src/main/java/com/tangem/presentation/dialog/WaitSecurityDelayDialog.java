package com.tangem.presentation.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import com.tangem.wallet.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dvol on 06.03.2018.
 */
public class WaitSecurityDelayDialog extends DialogFragment {
    ProgressBar progressBar;
    int msTimeout = 60000, msProgress = 0;
    Timer timer;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View v = inflater.inflate(R.layout.dialog_wait_pin2, null);

        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setMax(msTimeout);
        progressBar.setProgress(msProgress);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                progressBar.post(() -> {
                    int progress = WaitSecurityDelayDialog.this.progressBar.getProgress();
                    if (progress < WaitSecurityDelayDialog.this.progressBar.getMax()) {
                        WaitSecurityDelayDialog.this.progressBar.setProgress(progress + 1000);
                    }
                });
            }
        }, 1000, 1000);
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle("Security delay")
                .setView(v)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    public void setup(int msTimeout, int msProgress) {
        this.msTimeout = msTimeout;
        this.msProgress = msProgress;
    }

    public void setRemainingTimeout(final int msec) {
        progressBar.post(() -> {
            int progress = WaitSecurityDelayDialog.this.progressBar.getProgress();
            if (timer != null) {
                // we get delay latency from card for first time - don't change progress by timer, only by card answer
                progressBar.setMax(progress + msec);
                timer.cancel();
                timer = null;
            } else {
                int newProgress = progressBar.getMax() - msec;
                if (newProgress > progress) {
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setMax(progress + msec);
                }
            }
        });
    }

    static Timer timerToShowDelayDialog = null;
    static WaitSecurityDelayDialog instance = null;

    public static WaitSecurityDelayDialog getInstance() {
        if (instance == null) {
            instance = new WaitSecurityDelayDialog();
        }
        return instance;
    }

    private final static int MinRemainingDelayToShowDialog = 1000;
    private final static int DelayBeforeShowDialog = 5000;

    public static void onReadBeforeRequest(final Activity activity, final int timeout) {
        activity.runOnUiThread(() -> {
            if (timerToShowDelayDialog != null || timeout < DelayBeforeShowDialog + MinRemainingDelayToShowDialog)
                return;
            timerToShowDelayDialog = new Timer();
            timerToShowDelayDialog.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (WaitSecurityDelayDialog.instance != null) return;
                    instance = new WaitSecurityDelayDialog();
                    instance.setup(timeout, DelayBeforeShowDialog);
                    instance.setCancelable(false);
                    instance.show(activity.getFragmentManager(), "WaitSecurityDelayDialog");
                }
            }, DelayBeforeShowDialog);
        });
    }

    public static void onReadAfterRequest(final Activity activity) {
        activity.runOnUiThread(() -> {
            if (timerToShowDelayDialog == null) return;
            timerToShowDelayDialog.cancel();
            timerToShowDelayDialog = null;
        });
    }

    public static void OnReadWait(final Activity activity, final int msec) {
        activity.runOnUiThread(() -> {
            if (timerToShowDelayDialog != null) {
                timerToShowDelayDialog.cancel();
                timerToShowDelayDialog = null;
            }

            if (msec == 0) {
                if (instance != null) {
                    instance.dismiss();
                    instance = null;
                }
                return;
            }
            if (instance == null) {
                if (msec > MinRemainingDelayToShowDialog) {
                    instance = new WaitSecurityDelayDialog();
                    // 1000ms - card delay notification interval
                    instance.setup(msec + 1000, 1000);
                    instance.setCancelable(false);
                    instance.show(activity.getFragmentManager(), "WaitSecurityDelayDialog");
                }
            } else {
                instance.setRemainingTimeout(msec);
            }
        });
    }

}