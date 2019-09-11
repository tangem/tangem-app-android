package com.tangem.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.tangem.tangem_card.util.Log;
import com.tangem.wallet.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dvol on 06.03.2018.
 */
public class WaitSecurityDelayDialog extends DialogFragment {
    public static final String TAG = WaitSecurityDelayDialog.class.getSimpleName();

    private ProgressBar progressBar;
    private int msTimeout = 60000, msProgress = 0;
    private Timer timer;

    private static Timer timerToShowDelayDialog = null;
    private static WaitSecurityDelayDialog instance = null;

    private final static int minRemainingDelayToShowDialog = 1000;
    private final static int delayBeforeShowDialog = 5000;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

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
                .setTitle(R.string.security_delay)
                .setView(v)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
    }

    public static void onReadBeforeRequest(final FragmentActivity activity, final int timeout) {
        if (activity == null) return;

        Log.e(TAG, "onReadBeforeRequest callback(" + timeout + ")");
        activity.runOnUiThread(() -> {
            if (timerToShowDelayDialog != null || timeout < delayBeforeShowDialog + minRemainingDelayToShowDialog)
                return;
            timerToShowDelayDialog = new Timer();
            timerToShowDelayDialog.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (WaitSecurityDelayDialog.instance != null) return;
                    instance = new WaitSecurityDelayDialog();
                    instance.setup(timeout, delayBeforeShowDialog);
                    instance.setCancelable(false);
                    activity.getSupportFragmentManager().beginTransaction()
                            .add(instance, TAG).commitAllowingStateLoss();
                }
            }, delayBeforeShowDialog);
        });
    }

    public static void onReadAfterRequest(final Activity activity) {
        if (timerToShowDelayDialog == null) return;
        timerToShowDelayDialog.cancel();
        timerToShowDelayDialog = null;
    }

    public static void onReadWait(final FragmentActivity activity, final int msec) {
        Log.e(TAG, "onReadWait callback(" + msec + ")");
        if (timerToShowDelayDialog != null) {
            timerToShowDelayDialog.cancel();
            timerToShowDelayDialog = null;
        }

        if (msec == 0 && instance != null) {
            try {
                instance.dismissAllowingStateLoss();
                instance = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (activity == null) return;

        activity.runOnUiThread(() -> {
            Log.e(TAG, "onReadWait on ui thread(" + msec + ")");

            if (instance == null) {
                if (msec > delayBeforeShowDialog + minRemainingDelayToShowDialog) {
                    instance = new WaitSecurityDelayDialog();
                    // 1000ms - card delay notification interval
                    instance.setup(msec + 1000, 1000);
                    instance.setCancelable(false);
                    activity.getSupportFragmentManager().beginTransaction()
                            .add(instance, TAG).commitAllowingStateLoss();
                }
            } else
                instance.setRemainingTimeout(msec);
        });
    }

    private void setup(int msTimeout, int msProgress) {
        this.msTimeout = msTimeout;
        this.msProgress = msProgress;
    }

    private void setRemainingTimeout(final int msec) {
        progressBar.post(() -> {
            int progress = WaitSecurityDelayDialog.this.progressBar.getProgress();
            if (timer != null) {
                // we get delay latency from card for first time - don't change progress by timer, only by card answer
                progressBar.setMax(progress + msec);
                timer.cancel();
                timer = null;
            } else {
                int newProgress = progressBar.getMax() - msec;
                if (newProgress > progress)
                    progressBar.setProgress(newProgress);
                else
                    progressBar.setMax(progress + msec);
            }
        });
    }

}