package com.tangem.wallet;

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

import com.tangem.cardReader.CardProtocol;
import com.tangem.cardReader.NfcManager;
import com.tangem.cardReader.Util;

public class PurgeActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    public static final int RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER;

    private Tangem_Card mCard;
    private TextView tvCardID;
    private NfcManager mNfcManager;
    private static final String logTag = "Purge";
    private ProgressBar progressBar;
    private PurgeTask purgeTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purge);

        MainActivity.commonInit(getApplicationContext());

        mCard = new Tangem_Card(getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(getIntent().getExtras().getBundle("Card"));

        tvCardID = (TextView) findViewById(R.id.tvCardID);
        tvCardID.setText(mCard.getCIDDescription());

        mNfcManager = new NfcManager(this, this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
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
            Log.v(logTag, "UID: " + sUID);

            if (sUID.equals(mCard.getUID())) {
                isoDep.setTimeout(mCard.getPauseBeforePIN2() + 65000);
                purgeTask = new PurgeTask(isoDep, this);

                purgeTask.start();
            } else {
                Log.d(logTag, "Mismatch card UID (" + sUID + " instead of " + mCard.getUID() + ")");
                mNfcManager.IgnoreTag(isoDep.getTag());
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
        if (purgeTask != null) {
            purgeTask.cancel(true);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        // dismiss enable NFC dialog
        mNfcManager.onStop();
        if (purgeTask != null) {
            purgeTask.cancel(true);
        }
        super.onStop();
    }

//    @Override
//    public Dialog CreateNFCDialog(int id, AlertDialogWrapper.Builder builder, LayoutInflater li) {
//        return mNfcManager.onCreateDialog(id, builder, li);
//    }

    private class PurgeTask extends Thread {


        private String txOutAddress;

        IsoDep mIsoDep;
        CardProtocol.Notifications mNotifications;
        private boolean isCancelled = false;

        public PurgeTask(IsoDep isoDep, CardProtocol.Notifications notifications) {
            mIsoDep = isoDep;
            mNotifications = notifications;
        }

        @Override
        public void run() {
            if (mIsoDep == null) {
                return;
            }
            CardProtocol protocol = new CardProtocol(getBaseContext(), mIsoDep, mCard, mNotifications);

            mNotifications.OnReadStart(protocol);
            try {

                // for Samsung's bugs -
                // Workaround for the Samsung Galaxy S5 (since the
                // first connection always hangs on transceive).
                int timeout = mIsoDep.getTimeout();
                mIsoDep.connect();
                mIsoDep.close();
                mIsoDep.connect();
                mIsoDep.setTimeout(timeout);
                try {

                    mNotifications.OnReadProgress(protocol, 5);

                    Log.i("PurgeTask", "[-- Start purge --]");

                    if (isCancelled) return;

                    if (mCard.getPauseBeforePIN2() > 0) {
                        mNotifications.OnReadWait(mCard.getPauseBeforePIN2());
                    }

//                    try {
                        protocol.run_PurgeWallet(PINStorage.getPIN2());
//                    } finally {
//                        mNotifications.OnReadWait(0);
//                    }

                    mNotifications.OnReadProgress(protocol, 50);

                    protocol.run_Read();

                    mNotifications.OnReadProgress(protocol, 100);

                    if (isCancelled) return;

                } finally {
                    mNfcManager.IgnoreTag(mIsoDep.getTag());
                }
            } catch (Exception e) {
                e.printStackTrace();
                protocol.setError(e);

            } finally {
                Log.i("PurgeTask", "[-- Finish purge --]");
                mNotifications.OnReadFinish(protocol);
            }
        }

        public void cancel(Boolean AllowInterrupt) {
            try {
                if (this.isAlive()) {
                    isCancelled = true;
                    join(500);
                }
                if (this.isAlive() && AllowInterrupt) {
                    interrupt();
                    mNotifications.OnReadCancel();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    public void OnReadStart(CardProtocol cardProtocol) {
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(5);
            }
        });
    }

    public void OnReadFinish(final CardProtocol cardProtocol) {

        purgeTask = null;

        if (cardProtocol != null) {
            if (cardProtocol.getError() == null) {
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                        Intent intent = new Intent();
                        intent.putExtra("UID", cardProtocol.getCard().getUID());
                        intent.putExtra("Card", cardProtocol.getCard().getAsBundle());
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                });
            } else {
                if (cardProtocol.getError() instanceof CardProtocol.TangemException_InvalidPIN) {
                    progressBar.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(100);
                            progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                        }
                    });
                    progressBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                progressBar.setProgress(0);
                                progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                                progressBar.setVisibility(View.INVISIBLE);
                                Intent intent = new Intent();
                                intent.putExtra("UID", cardProtocol.getCard().getUID());
                                intent.putExtra("Card", cardProtocol.getCard().getAsBundle());
                                intent.putExtra("message", "Cannot erase wallet. Make sure you enter correct PIN2!");
                                setResult(RESULT_INVALID_PIN, intent);
                                finish();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, 500);
                    return;
                } else {
                    progressBar.post(new Runnable() {
                        @Override
                        public void run() {
                            if (cardProtocol.getError() instanceof CardProtocol.TangemException_ExtendedLengthNotSupported) {
                                if (!NoExtendedLengthSupportDialog.allreadyShowed) {
                                    new NoExtendedLengthSupportDialog().show(getFragmentManager(), "NoExtendedLengthSupportDialog");
                                }
                            } else {
                                Toast.makeText(getBaseContext(), "Try to scan again", Toast.LENGTH_LONG).show();
                            }
                            progressBar.setProgress(100);
                            progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                        }
                    });
                }
            }
        }

        progressBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    progressBar.setProgress(0);
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                    progressBar.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 500);
    }

    public void OnReadProgress(CardProtocol protocol, final int progress) {
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(progress);
            }
        });
    }

    public void OnReadCancel() {

        purgeTask = null;

        progressBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    progressBar.setProgress(0);
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                    progressBar.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

