package com.tangem.presentation.activity;import android.app.Activity;import android.content.Intent;import android.content.res.ColorStateList;import android.graphics.Color;import android.nfc.NfcAdapter;import android.nfc.Tag;import android.nfc.tech.IsoDep;import android.os.Bundle;import android.support.v7.app.AppCompatActivity;import android.util.Log;import android.view.KeyEvent;import android.view.View;import android.widget.ProgressBar;import android.widget.TextView;import android.widget.Toast;import com.tangem.cardReader.CardProtocol;import com.tangem.cardReader.NfcManager;import com.tangem.cardReader.Util;import com.tangem.wallet.BTCUtils;import com.tangem.wallet.Blockchain;import com.tangem.wallet.CoinEngine;import com.tangem.wallet.CoinEngineFactory;import com.tangem.wallet.LastSignStorage;import com.tangem.wallet.NoExtendedLengthSupportDialog;import com.tangem.wallet.R;import com.tangem.wallet.Tangem_Card;import com.tangem.wallet.WaitSecurityDelayDialog;public class SignPaymentActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, CardProtocol.Notifications {    private static final int REQUEST_CODE_SEND_PAYMENT = 1;    public static final int RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER;    private Tangem_Card mCard;    private TextView tvCardID;    private NfcManager mNfcManager;    private static final String logTag = "SignPayment";    private ProgressBar progressBar;    private SignPaymentTask signPaymentTask;    private String amountStr;    private String feeStr;    private String outAddressStr;    private boolean lastReadSuccess = true;    @Override    protected void onCreate(Bundle savedInstanceState) {        super.onCreate(savedInstanceState);        setContentView(R.layout.activity_sign_payment);        MainActivity.commonInit(getApplicationContext());        mCard = new Tangem_Card(getIntent().getStringExtra("UID"));        mCard.LoadFromBundle(getIntent().getExtras().getBundle("Card"));        amountStr = getIntent().getStringExtra("Amount");        feeStr = getIntent().getStringExtra("Fee");        outAddressStr = getIntent().getStringExtra("Wallet");        tvCardID = findViewById(R.id.tvCardID);        tvCardID.setText(mCard.getCIDDescription());        mNfcManager = new NfcManager(this, this);        progressBar = findViewById(R.id.progressBar);        progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));        progressBar.setVisibility(View.INVISIBLE);    }    @Override    public void onTagDiscovered(Tag tag) {        try {            // get IsoDep handle and run cardReader thread            final IsoDep isoDep = IsoDep.get(tag);            if (isoDep == null) {                throw new CardProtocol.TangemException(getString(R.string.wrong_tag_err));            }            byte UID[] = tag.getId();            String sUID = Util.byteArrayToHexString(UID);            Log.v(logTag, "UID: " + sUID);            if (sUID.equals(mCard.getUID())) {                if (lastReadSuccess) {                    isoDep.setTimeout(mCard.getPauseBeforePIN2() + 5000);                } else {                    isoDep.setTimeout(mCard.getPauseBeforePIN2() + 65000);                }                signPaymentTask = new SignPaymentTask(isoDep, this, amountStr, feeStr, outAddressStr);                signPaymentTask.start();            } else {                Log.d(logTag, "Mismatch card UID (" + sUID + " instead of " + mCard.getUID() + ")");                mNfcManager.IgnoreTag(isoDep.getTag());                return;            }        } catch (Exception e) {            e.printStackTrace();        }    }    @Override    public void onResume() {        super.onResume();        mNfcManager.onResume();    }    @Override    public void onPause() {        mNfcManager.onPause();        if (signPaymentTask != null) {            signPaymentTask.cancel(true);        }        super.onPause();    }    @Override    public void onStop() {        // dismiss enable NFC dialog        mNfcManager.onStop();        if (signPaymentTask != null) {            signPaymentTask.cancel(true);        }        super.onStop();    }//    @Override//    public Dialog CreateNFCDialog(int id, AlertDialogWrapper.Builder builder, LayoutInflater li) {//        return mNfcManager.onCreateDialog(id, builder, li);//    }    private class SignPaymentTask extends Thread {        String txAmount = "";        String txFee = "";        public void SetTransactionValue(String amount, String fee) {            txAmount = amount;            txFee = fee;        }        private String txOutAddress;        IsoDep mIsoDep;        CardProtocol.Notifications mNotifications;        private boolean isCancelled = false;        public SignPaymentTask(IsoDep isoDep, CardProtocol.Notifications notifications, String amount, String fee, String outAddress) {            mIsoDep = isoDep;            mNotifications = notifications;            txOutAddress = outAddress;            SetTransactionValue(amount, fee);        }        @Override        public void run() {            if (mIsoDep == null) {                return;            }            CardProtocol protocol = new CardProtocol(getBaseContext(), mIsoDep, mCard, mNotifications);            mNotifications.OnReadStart(protocol);            try {                // for Samsung's bugs -                // Workaround for the Samsung Galaxy S5 (since the                // first connection always hangs on transceive).                int timeout = mIsoDep.getTimeout();                mIsoDep.connect();                mIsoDep.close();                mIsoDep.connect();                mIsoDep.setTimeout(timeout);                try {                    mNotifications.OnReadProgress(protocol, 5);                    Log.i("SignPaymentTask", "[-- Start sign payment --]");                    if (isCancelled) return;                    protocol.run_VerifyCard();                    Log.i("SignPaymentTask", "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());                    mNotifications.OnReadProgress(protocol, 30);                    if (isCancelled) return;////                    if (mCard.getBlockchain() == Blockchain.Ethereum) {//                        SignETH_TX(protocol);//                    } else {//                        SignBTC_TX(protocol);//                    }                    CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());                    if (engine != null) {                        if (mCard.getPauseBeforePIN2() > 0) {                            mNotifications.OnReadWait(mCard.getPauseBeforePIN2());                        }                        byte[] tx;//                        try {                        tx = engine.Sign(txFee, txAmount, txOutAddress, mCard, protocol);//                        }//                        finally {//                            mNotifications.OnReadWait(0);//                        }                        if (tx != null) {                            String txStr = BTCUtils.toHex(tx);                            if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet || mCard.getBlockchain() == Blockchain.Token) {                                txStr = String.format("0x%s", txStr);                            }                            LastSignStorage.setLastTX(mCard.getWallet(), txStr);                            Intent intent = new Intent(getBaseContext(), SendTransactionActivity.class);                            intent.putExtra("UID", mCard.getUID());                            intent.putExtra("Card", mCard.getAsBundle());                            intent.putExtra("TX", txStr);                            startActivityForResult(intent, REQUEST_CODE_SEND_PAYMENT);                        }                    }                    mNotifications.OnReadProgress(protocol, 100);                    if (isCancelled) return;                } finally {                    mNfcManager.IgnoreTag(mIsoDep.getTag());                }            } catch (CardProtocol.TangemException_InvalidPIN e) {                e.printStackTrace();                protocol.setError(e);            } catch (Exception e) {                e.printStackTrace();                protocol.setError(e);            } finally {                Log.i("SignPaymentTask", "[-- Finish sign payment --]");                mNotifications.OnReadFinish(protocol);            }        }        public void cancel(Boolean AllowInterrupt) {            try {                if (this.isAlive()) {                    isCancelled = true;                    join(500);                }                if (this.isAlive() && AllowInterrupt) {                    interrupt();                    mNotifications.OnReadCancel();                }            } catch (Exception e) {                e.printStackTrace();            }        }    }    public void OnReadStart(CardProtocol cardProtocol) {        progressBar.post(new Runnable() {            @Override            public void run() {                progressBar.setVisibility(View.VISIBLE);                progressBar.setProgress(5);            }        });    }    public void OnReadFinish(final CardProtocol cardProtocol) {        signPaymentTask = null;        if (cardProtocol != null) {            if (cardProtocol.getError() == null) {                progressBar.post(new Runnable() {                    @Override                    public void run() {                        progressBar.setProgress(100);                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));                    }                });            } else {                lastReadSuccess = false;                if (cardProtocol.getError().getClass().equals(CardProtocol.TangemException_InvalidPIN.class)) {                    progressBar.post(new Runnable() {                        @Override                        public void run() {                            progressBar.setProgress(100);                            progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));                        }                    });                    progressBar.postDelayed(new Runnable() {                        @Override                        public void run() {                            try {                                progressBar.setProgress(0);                                progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));                                progressBar.setVisibility(View.INVISIBLE);                                Intent intent = new Intent();                                intent.putExtra("message", "Cannot sign transaction. Make sure you enter correct PIN2!");                                intent.putExtra("UID", cardProtocol.getCard().getUID());                                intent.putExtra("Card", cardProtocol.getCard().getAsBundle());                                setResult(RESULT_INVALID_PIN, intent);                                finish();                            } catch (Exception e) {                                e.printStackTrace();                            }                        }                    }, 500);                    return;                } else {                    progressBar.post(new Runnable() {                        @Override                        public void run() {                            if (cardProtocol.getError() instanceof CardProtocol.TangemException_ExtendedLengthNotSupported) {                                if (!NoExtendedLengthSupportDialog.allreadyShowed) {                                    new NoExtendedLengthSupportDialog().show(getFragmentManager(), "NoExtendedLengthSupportDialog");                                }                            } else {                                Toast.makeText(getBaseContext(), "Try to scan again", Toast.LENGTH_LONG).show();                            }                            progressBar.setProgress(100);                            progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));                        }                    });                }            }        }        progressBar.postDelayed(new Runnable() {            @Override            public void run() {                try {                    progressBar.setProgress(0);                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));                    progressBar.setVisibility(View.INVISIBLE);                } catch (Exception e) {                    e.printStackTrace();                }            }        }, 500);    }    public void OnReadProgress(CardProtocol protocol, final int progress) {        progressBar.post(new Runnable() {            @Override            public void run() {                progressBar.setProgress(progress);            }        });    }    public void OnReadCancel() {        signPaymentTask = null;        progressBar.postDelayed(new Runnable() {            @Override            public void run() {                try {                    progressBar.setProgress(0);                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));                    progressBar.setVisibility(View.INVISIBLE);                } catch (Exception e) {                    e.printStackTrace();                }            }        }, 500);    }    @Override    public void OnReadWait(final int msec) {        WaitSecurityDelayDialog.OnReadWait(this, msec);    }    @Override    public void OnReadBeforeRequest(int timeout) {        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout);    }    @Override    public void OnReadAfterRequest() {        WaitSecurityDelayDialog.onReadAfterRequest(this);    }    @Override    public boolean onKeyDown(int keyCode, KeyEvent event) {        switch (keyCode) {            case KeyEvent.KEYCODE_BACK:                Intent intent = new Intent();                intent.putExtra("message", "Operation canceled by user");                setResult(Activity.RESULT_CANCELED, intent);                finish();                return true;        }        return super.onKeyDown(keyCode, event);    }    @Override    protected void onActivityResult(int requestCode, int resultCode, Intent data) {        if (requestCode == REQUEST_CODE_SEND_PAYMENT) {            setResult(resultCode, data);            finish();            return;        }        super.onActivityResult(requestCode, resultCode, data);    }}