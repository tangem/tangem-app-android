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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.cardReader.Util;
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog;
import com.tangem.wallet.R;
import com.tangem.wallet.Tangem_Card;
import com.tangem.wallet.VerifyCardTask;
import com.tangem.presentation.dialog.WaitSecurityDelayDialog;

public class EmptyWalletActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    private static final int REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY = 2;
    private static final int REQUEST_CODE_REQUEST_PIN2 = 3;
    private static final int REQUEST_CODE_VERIFY_CARD = 4;
    Tangem_Card mCard;
    TextView tvCardID, tvIssuer, tvIssuerData, tvBlockchain;
    ProgressBar progressBar;
    ImageView ivBlockchain, ivPIN, ivPIN2orSecurityDelay, ivDeveloperVersion;

    private NfcManager mNfcManager;
    private final String logTag = "EmptyWalletActivity";
    private boolean lastReadSuccess = true;
    private VerifyCardTask verifyCardTask = null;
    private int requestPIN2Count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty_wallet);

        MainActivity.commonInit(getApplicationContext());
        mNfcManager = new NfcManager(this, this);

        mCard = new Tangem_Card(getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(getIntent().getExtras().getBundle("Card"));

        tvCardID = findViewById(R.id.tvCardID);
        tvCardID.setText(mCard.getCIDDescription());

        tvIssuer = findViewById(R.id.tvIssuer);
        tvIssuerData = findViewById(R.id.tvIssuerData);
        tvBlockchain = findViewById(R.id.tvBlockchain);

        tvIssuer.setText(mCard.getIssuerDescription());
        tvIssuerData.setText(mCard.getIssuerDataDescription());

        //tvBlockchain.setText(mCard.getBlockchain().getOfficialName());
        tvBlockchain.setText(mCard.getBlockchainName());
        progressBar = findViewById(R.id.progressBar);

        ivBlockchain = findViewById(R.id.imgBlockchain);
        ivPIN = findViewById(R.id.imgPIN);
        ivPIN2orSecurityDelay = findViewById(R.id.imgPIN2orSecurityDelay);
        ivDeveloperVersion = findViewById(R.id.imgDeveloperVersion);

        ivBlockchain.setImageResource(mCard.getBlockchain().getImageResource(this, mCard.getTokenSymbol()));

        if (mCard.useDefaultPIN1()) {
            ivPIN.setImageResource(R.drawable.unlock_pin1);
            ivPIN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(EmptyWalletActivity.this, "This banknote is protected by default PIN1 code", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            ivPIN.setImageResource(R.drawable.lock_pin1);
            ivPIN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(EmptyWalletActivity.this, "This banknote is protected by user's PIN1 code", Toast.LENGTH_LONG).show();
                }
            });
        }

        if (mCard.getPauseBeforePIN2() > 0 && (mCard.useDefaultPIN2() || !mCard.useSmartSecurityDelay())) {
            ivPIN2orSecurityDelay.setImageResource(R.drawable.timer);
            ivPIN2orSecurityDelay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(EmptyWalletActivity.this, String.format("This banknote will enforce %.0f seconds security delay for all operations requiring PIN2 code", mCard.getPauseBeforePIN2() / 1000.0), Toast.LENGTH_LONG).show();
                }
            });

        } else if (mCard.useDefaultPIN2()) {
            ivPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2);
            ivPIN2orSecurityDelay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(EmptyWalletActivity.this, "This banknote is protected by default PIN2 code", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            ivPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2);
            ivPIN2orSecurityDelay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(EmptyWalletActivity.this, "This banknote is protected by user's PIN2 code", Toast.LENGTH_LONG).show();
                }
            });
        }


        if (mCard.useDevelopersFirmware()) {
            ivDeveloperVersion.setImageResource(R.drawable.ic_developer_version);
            ivDeveloperVersion.setVisibility(View.VISIBLE);
            ivDeveloperVersion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(EmptyWalletActivity.this, "Unlocked banknote, only for development use", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            ivDeveloperVersion.setVisibility(View.INVISIBLE);
        }

        Button btnNewWallet = findViewById(R.id.btnNewWallet);
        btnNewWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CreateSelectBlockchainDialog();
                requestPIN2Count = 0;
                Intent intent = new Intent(getBaseContext(), RequestPINActivity.class);
                intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                intent.putExtra("UID", mCard.getUID());
                intent.putExtra("Card", mCard.getAsBundle());
                startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2);
            }
        });

        if (getIntent().getExtras().containsKey(NfcAdapter.EXTRA_TAG)) {
            Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                onTagDiscovered(tag);
            }
        }
    }


    private void doCreateNewWallet() {
        Intent intent = new Intent(this, CreateNewWalletActivity.class);

        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());

//        intent.putExtra("newPIN",mCard.getPIN());
//        intent.putExtra("newPIN2","12345678");
        startActivityForResult(intent, REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {

                if (data != null) {
                    data.putExtra("modification", "updateAndViewCard");
                    data.putExtra("updateDelay", 0);
                    setResult(Activity.RESULT_OK, data);
                }
                finish();
            } else {
                if (data != null && data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
                    Tangem_Card updatedCard = new Tangem_Card(data.getStringExtra("UID"));
                    updatedCard.LoadFromBundle(data.getBundleExtra("Card"));
                    mCard = updatedCard;
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++;
                    Intent intent = new Intent(getBaseContext(), RequestPINActivity.class);
                    intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                    intent.putExtra("UID", mCard.getUID());
                    intent.putExtra("Card", mCard.getAsBundle());
                    startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2);
                    return;
                }
            }
            setResult(resultCode, data);
            finish();
        } else if (requestCode == REQUEST_CODE_REQUEST_PIN2) {
            if (resultCode == Activity.RESULT_OK) {
                doCreateNewWallet();
            }
        }

    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            final IsoDep isoDep = IsoDep.get(tag);
            if (isoDep == null) {
                throw new CardProtocol.TangemException(getString(R.string.wrong_tag_err));
            }
            byte UID[] = tag.getId();
            String sUID = Util.byteArrayToHexString(UID);
            if (!mCard.getUID().equals(sUID)) {
                Log.d(logTag, "Invalid UID: " + sUID);
                mNfcManager.IgnoreTag(isoDep.getTag());
                return;
            } else {
                Log.v(logTag, "UID: " + sUID);
            }

            if (lastReadSuccess) {
                isoDep.setTimeout(1000);
            } else {
                isoDep.setTimeout(65000);
            }
            //lastTag = tag;
            verifyCardTask = new VerifyCardTask(this, mCard, mNfcManager, isoDep, this);
            verifyCardTask.start();
        } catch (Exception e) {
            e.printStackTrace();
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

        verifyCardTask = null;

        if (cardProtocol != null) {
            if (cardProtocol.getError() == null) {
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                        Intent intent = new Intent(EmptyWalletActivity.this, VerifyCardActivity.class);
                        // TODO обновить карту mCard
                        intent.putExtra("UID", cardProtocol.getCard().getUID());
                        intent.putExtra("Card", cardProtocol.getCard().getAsBundle());
                        startActivityForResult(intent, REQUEST_CODE_VERIFY_CARD);
                        //addCard(cardProtocol.getCard());
                    }
                });
            } else {
                // remove last UIDs because of error and no card read
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        lastReadSuccess = false;
                        if (cardProtocol.getError() instanceof CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allreadyShowed) {
                                new NoExtendedLengthSupportDialog().show(getFragmentManager(), "NoExtendedLengthSupportDialog");
                            }
                        } else {
                            Toast.makeText(EmptyWalletActivity.this, "Try to scan again", Toast.LENGTH_LONG).show();
                        }
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                    }
                });
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

        verifyCardTask = null;

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
    public void OnReadWait(int msec) {
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


    @Override
    public void onResume() {
        super.onResume();
        mNfcManager.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mNfcManager.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mNfcManager.onStop();
    }
}
