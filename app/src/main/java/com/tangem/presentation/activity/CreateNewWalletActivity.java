package com.tangem.presentation.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tangem.data.nfc.CreateNewWalletTask;
import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog;
import com.tangem.presentation.dialog.WaitSecurityDelayDialog;
import com.tangem.util.Util;
import com.tangem.wallet.R;

public class CreateNewWalletActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, CardProtocol.Notifications {
    public static final String TAG = CreateNewWalletActivity.class.getSimpleName();

    public static final int RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER;
    private TangemCard mCard;
    private NfcManager mNfcManager;

    private ProgressBar progressBar;
    private CreateNewWalletTask createNewWalletTask;
    private boolean lastReadSuccess = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_wallet);

        MainActivity.commonInit(getApplicationContext());
        mCard = new TangemCard(getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(getIntent().getExtras().getBundle("Card"));

        TextView tvCardID = findViewById(R.id.tvCardID);
        tvCardID.setText(mCard.getCIDDescription());

        mNfcManager = new NfcManager(this, this);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            // get IsoDep handle and run cardReader thread
            final IsoDep isoDep = IsoDep.get(tag);
            if (isoDep == null) {
                throw new CardProtocol.TangemException(getString(R.string.wrong_tag_err));
            }
            byte UID[] = tag.getId();
            String sUID = Util.byteArrayToHexString(UID);
            Log.v(TAG, "UID: " + sUID);

            if (sUID.equals(mCard.getUID())) {
                if (lastReadSuccess) {
                    isoDep.setTimeout(mCard.getPauseBeforePIN2() + 5000);
                } else {
                    isoDep.setTimeout(mCard.getPauseBeforePIN2() + 65000);
                }
                createNewWalletTask = new CreateNewWalletTask(this, mCard, mNfcManager, isoDep, this);
                createNewWalletTask.start();
            } else {
                Log.d(TAG, "Mismatch card UID (" + sUID + " instead of " + mCard.getUID() + ")");
                mNfcManager.ignoreTag(isoDep.getTag());
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mNfcManager.onResume();
    }

    @Override
    public void onPause() {
        mNfcManager.onPause();
        if (createNewWalletTask != null) {
            createNewWalletTask.cancel(true);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        // dismiss enable NFC dialog
        mNfcManager.onStop();
        if (createNewWalletTask != null) {
            createNewWalletTask.cancel(true);
        }
        super.onStop();
    }

    public void OnReadStart(CardProtocol cardProtocol) {
        progressBar.post(() -> {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(5);
        });
    }

    public void OnReadFinish(final CardProtocol cardProtocol) {

        createNewWalletTask = null;

        if (cardProtocol != null) {
            if (cardProtocol.getError() == null) {
                progressBar.post(() -> {
                    progressBar.setProgress(100);
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                    Intent intent = new Intent();
                    intent.putExtra("UID", cardProtocol.getCard().getUID());
                    intent.putExtra("Card", cardProtocol.getCard().getAsBundle());
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                });
            } else {
                lastReadSuccess = false;
                if (cardProtocol.getError() instanceof CardProtocol.TangemException_InvalidPIN) {
                    progressBar.post(() -> {
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                    });
                    progressBar.postDelayed(() -> {
                        try {
                            progressBar.setProgress(0);
                            progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                            progressBar.setVisibility(View.INVISIBLE);
                            Intent intent = new Intent();
                            intent.putExtra("message", "Cannot create wallet. Make sure you enter correct PIN2!");
                            intent.putExtra("UID", cardProtocol.getCard().getUID());
                            intent.putExtra("Card", cardProtocol.getCard().getAsBundle());
                            setResult(RESULT_INVALID_PIN, intent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, 500);
                    return;
                } else {
                    progressBar.post(() -> {
                        if (cardProtocol.getError() instanceof CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allreadyShowed) {
                                new NoExtendedLengthSupportDialog().show(getFragmentManager(), NoExtendedLengthSupportDialog.TAG);
                            }
                        } else {
                            Toast.makeText(getBaseContext(), R.string.try_to_scan_again, Toast.LENGTH_SHORT).show();
                        }
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                    });
                }
            }
        }

        progressBar.postDelayed(() -> {
            try {
                progressBar.setProgress(0);
                progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                progressBar.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500);
    }

    public void OnReadProgress(CardProtocol protocol, final int progress) {
        progressBar.post(() -> progressBar.setProgress(progress));
    }

    public void OnReadCancel() {

        createNewWalletTask = null;

        progressBar.postDelayed(() -> {
            try {
                progressBar.setProgress(0);
                progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                progressBar.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500);
    }

    @Override
    public void OnReadWait(final int msec) {
        WaitSecurityDelayDialog.OnReadWait(this, msec);
    }

    @Override
    public void OnReadBeforeRequest(int timeout) {
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout);
    }

    @Override
    public void OnReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(this);
    }

}