package com.tangem.presentation.activity;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.request.FeeRequest;
import com.tangem.data.network.request.InfuraRequest;
import com.tangem.data.network.task.confirm_payment.ConnectFeeTask;
import com.tangem.data.network.task.confirm_payment.ConnectTask;
import com.tangem.data.network.task.confirm_payment.ETHRequestTask;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.SharedData;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.domain.wallet.Transaction;
import com.tangem.domain.wallet.UnspentOutputInfo;
import com.tangem.util.BTCUtils;
import com.tangem.util.DerEncodingUtil;
import com.tangem.util.FormatUtil;
import com.tangem.util.Util;
import com.tangem.wallet.R;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ConfirmPaymentActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final int REQUEST_CODE_SIGN_PAYMENT = 1;
    private static final int REQUEST_CODE_REQUEST_PIN2 = 2;
    public Button btnSend;
    public boolean feeRequestSuccess = false;
    public boolean balanceRequestSuccess = false;
    private EditText etWallet;
    //    private TextView tvAmountEquivalent;
    private TextView tvFeeEquivalent;
    public EditText etAmount;
    public EditText etFee;
    private ImageView ivCamera;
    public TangemCard mCard;
    public RadioGroup rgFee;
    public String minFee = null, maxFee = null, normalFee = null;
    public Long minFeeInInternalUnits = 0L;
    private NfcManager mNfcManager;
    private int requestPIN2Count = 0;
    public ProgressBar progressBar;
    public boolean nodeCheck = false;
    public Date dtVerifyed = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_payment);

        MainActivity.commonInit(getApplicationContext());
        mNfcManager = new NfcManager(this, this);

        mCard = new TangemCard(getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(getIntent().getExtras().getBundle("Card"));

        progressBar = findViewById(R.id.progressBar);
        btnSend = findViewById(R.id.btnSend);
        etWallet = findViewById(R.id.etWallet);
        TextView tvCardID = findViewById(R.id.tvCardID);
        TextView tvBalance = findViewById(R.id.tvBalance);
        TextView tvCurrency = findViewById(R.id.tvCurrency);
        TextView tvCurrency2 = findViewById(R.id.tvCurrency2);
        etAmount = findViewById(R.id.etAmount);
        etFee = findViewById(R.id.etFee);
//        TextView tvBalanceEquivalent = findViewById(R.id.tvBalanceEquivalent);
//        tvAmountEquivalent = findViewById(R.id.tvAmountEquivalent);
        tvFeeEquivalent = findViewById(R.id.tvFeeEquivalent);
        ivCamera = findViewById(R.id.ivCamera);
        rgFee = findViewById(R.id.rgFee);

        rgFee.setOnCheckedChangeListener((group, checkedId) -> doSetFee(checkedId));

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                try {
//
//                    CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
//                    tvAmountEquivalent.setText(engine.GetAmountEqualentDescriptor(mCard, etAmount.getText().toString()));
//                    if (!mCard.getAmountEquivalentDescriptionAvailable()) {
//                        tvAmountEquivalent.setError("Service unavailable");
//                    } else {
//                        tvAmountEquivalent.setError(null);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    tvAmountEquivalent.setText("");
//                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {

                    CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
                    String eqFee = engine.EvaluteFeeEquivalent(mCard, etFee.getText().toString());
                    tvFeeEquivalent.setText(eqFee);

                    if (!mCard.getAmountEquivalentDescriptionAvailable()) {
                        tvFeeEquivalent.setError("Service unavailable");
                    } else {
                        tvFeeEquivalent.setError(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    tvFeeEquivalent.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
        //tvBalance.setText(engine.GetBalanceWithAlter(mCard));
        if (mCard.getBlockchain() == Blockchain.Token) {
            Spanned html = Html.fromHtml(engine.GetBalanceWithAlter(mCard));
            tvBalance.setText(html);
        } else {
            tvBalance.setText(engine.GetBalanceWithAlter(mCard));
        }
        etAmount.setText(getIntent().getStringExtra(SignPaymentActivity.EXTRA_AMOUNT));
        tvCurrency.setText(engine.GetBalanceCurrency(mCard));
        tvCurrency2.setText(engine.GetFeeCurrency());

        tvCardID.setText(mCard.getCIDDescription());

        //tvBalanceEquivalent.setText(mCard.getBalanceEquivalentDescription());
//        tvBalanceEquivalent.setText(engine.GetBalanceEquivalent(mCard));
//        if (!mCard.getAmountEquivalentDescriptionAvailable()) {
//            tvBalanceEquivalent.setError("Service unavailable");
//        } else {
//            tvBalanceEquivalent.setError(null);
//        }

        etWallet.setText(getIntent().getStringExtra("Wallet"));

        etFee.setText("?");

        btnSend.setVisibility(View.INVISIBLE);
        feeRequestSuccess = false;
        balanceRequestSuccess = false;

        btnSend.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -1);

            if (dtVerifyed == null || dtVerifyed.before(calendar.getTime())) {
                finishActivityWithError(Activity.RESULT_CANCELED, "The obtained data is outdated! Try again");
                return;
            }

            CoinEngine engineCoin = CoinEngineFactory.Create(mCard.getBlockchain());

            if (engineCoin.IsNeedCheckNode() && !nodeCheck) {
                Toast.makeText(getBaseContext(), "Cannot reach current active blockchain node. Try again", Toast.LENGTH_LONG).show();
                return;
            }
            String txFee = etFee.getText().toString();
            String txAmount = etAmount.getText().toString();


            if (!engineCoin.HasBalanceInfo(mCard)) {
                finishActivityWithError(Activity.RESULT_CANCELED, "Cannot check balance! No connection with blockchain nodes");
                return;
            } else if (!engineCoin.IsBalanceNotZero(mCard)) {
                finishActivityWithError(Activity.RESULT_CANCELED, "The wallet is empty");
                return;
            } else if (!engineCoin.CheckUnspentTransaction(mCard)) {
                //else if (mCard.getUnspentTransactions().size() == 0 && mCard.getBlockchain() != Blockchain.Ethereum) {
                finishActivityWithError(Activity.RESULT_CANCELED, "Please wait for confirmation of incoming transaction");
                return;
            }

            if (!engineCoin.CheckAmountValie(mCard, txAmount, txFee, minFeeInInternalUnits)) {
                finishActivityWithError(Activity.RESULT_CANCELED, "Fee exceeds payment amount. Enter correct value and repeat sending.");
                return;
            }

            requestPIN2Count = 0;
            Intent intent = new Intent(getBaseContext(), RequestPINActivity.class);
            intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
            intent.putExtra("UID", mCard.getUID());
            intent.putExtra("Card", mCard.getAsBundle());
            startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2);
        });

        if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet || mCard.getBlockchain() == Blockchain.Token) {
            ETHRequestTask task = new ETHRequestTask(ConfirmPaymentActivity.this, mCard.getBlockchain());
            InfuraRequest req = InfuraRequest.GetGasPrise(mCard.getWallet());
            req.setID(67);
            req.setBlockchain(mCard.getBlockchain());
            rgFee.setEnabled(false);
            task.execute(req);

        } else {
            rgFee.setEnabled(true);

            SharedData data = new SharedData(SharedData.COUNT_REQUEST);

            CoinEngine engineCoin = CoinEngineFactory.Create(mCard.getBlockchain());

            for (int i = 0; i < data.allRequest; ++i) {

                String nodeAddress = engineCoin.GetNextNode(mCard);
                int nodePort = engineCoin.GetNextNodePort(mCard);
                ConnectTask connectTaskEx = new ConnectTask(ConfirmPaymentActivity.this, nodeAddress, nodePort, data);
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(mCard.getWallet()));
            }

//            String nodeAddress = engineCoin.GetNode(mCard);
//            int nodePort = engineCoin.GetNodePort(mCard);
//            ConnectTask connectTask = new ConnectTask(nodeAddress, nodePort, data);
//
//
//            connectTask.execute(ElectrumRequest.GetFee(mCard.getWallet()));

            int calcSize = 256;
            try {
                calcSize = BuildSize(etWallet.getText().toString(), "0.00", etAmount.getText().toString());
            } catch (Exception ex) {
                Log.e("Build Fee error", ex.getMessage());
            }

            SharedData sharedFee = new SharedData(SharedData.COUNT_REQUEST);

            progressBar.setVisibility(View.VISIBLE);
            for (int i = 0; i < SharedData.COUNT_REQUEST; ++i) {

                ConnectFeeTask feeTask = new ConnectFeeTask(ConfirmPaymentActivity.this, sharedFee);

                feeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        FeeRequest.GetFee(mCard.getWallet(), calcSize, FeeRequest.NORMAL),
                        FeeRequest.GetFee(mCard.getWallet(), calcSize, FeeRequest.MINIMAL),
                        FeeRequest.GetFee(mCard.getWallet(), calcSize, FeeRequest.PRIORITY));
            }
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_PAYMENT) {
            if (data != null && data.getExtras() != null) {
                if (data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
                    TangemCard updatedCard = new TangemCard(data.getStringExtra("UID"));
                    updatedCard.LoadFromBundle(data.getBundleExtra("Card"));
                    mCard = updatedCard;
                }
            }
            if (resultCode == SignPaymentActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                requestPIN2Count++;
                Intent intent = new Intent(getBaseContext(), RequestPINActivity.class);
                intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                intent.putExtra("UID", mCard.getUID());
                intent.putExtra("Card", mCard.getAsBundle());
                startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2);
                return;
            }
            setResult(resultCode, data);
            finish();
        } else if (requestCode == REQUEST_CODE_REQUEST_PIN2) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(getBaseContext(), SignPaymentActivity.class);
                intent.putExtra("UID", mCard.getUID());
                intent.putExtra("Card", mCard.getAsBundle());
                intent.putExtra("Wallet", etWallet.getText().toString());
                intent.putExtra(SignPaymentActivity.EXTRA_AMOUNT, etAmount.getText().toString());
                intent.putExtra("Fee", etFee.getText().toString());
                startActivityForResult(intent, REQUEST_CODE_SIGN_PAYMENT);
            } else {
                Toast.makeText(getBaseContext(), "PIN2 is required to sign the payment", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Intent intent = new Intent();
//                intent.putExtra("message", "Operation canceled");
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void finishActivityWithError(int errorCode, String message) {
        //Snackbar.make(etFee, message, Snackbar.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.putExtra("message", message);
        setResult(errorCode, intent);
        finish();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            Log.w(getClass().getName(), "Ignore discovered tag!");
            mNfcManager.ignoreTag(tag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int BuildSize(String outputAddress, String outFee, String outAmount) throws Exception {
        String myAddress = mCard.getWallet();
        String changeAddress = myAddress; //"n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi";
        byte[] pbKey = mCard.getWalletPublicKey();
        byte[] pbComprKey = mCard.getWalletPublicKeyRar();

        // Build script for our address
        List<TangemCard.UnspentTransaction> rawTxList = mCard.getUnspentTransactions();
        byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(myAddress).bytes;

        // Collect unspent
        ArrayList<UnspentOutputInfo> unspentOutputs = BTCUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend);

        long fullAmount = 0;
        for (int i = 0; i < unspentOutputs.size(); ++i) {
            fullAmount += unspentOutputs.get(i).value;
        }

        // Get first unspent
        UnspentOutputInfo outPut = unspentOutputs.get(0);
        int outPutIndex = outPut.outputIndex;

        // get prev TX id;
        String prevTXID = rawTxList.get(0).txID;//"f67b838d6e2c0c587f476f583843e93ff20368eaf96a798bdc25e01f53f8f5d2";

        long fees = FormatUtil.ConvertStringToLong(outFee);
        long amount = FormatUtil.ConvertStringToLong(outAmount);
        amount = amount - fees;

        long change = fullAmount - fees - amount;

        if (amount + fees > fullAmount) {
            throw new Exception(String.format("Balance (%d) < amount (%d) + (%d)", fullAmount, change, amount));
        }

        byte[][] hashesForSign = new byte[unspentOutputs.size()][];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            byte[] newTX = BTCUtils.buildTXForSign(myAddress, outputAddress, changeAddress, unspentOutputs, i, amount, change);

            byte[] hashData = Util.calculateSHA256(newTX);
            byte[] doubleHashData = Util.calculateSHA256(hashData);

            Log.e("TX_BODY_1", BTCUtils.toHex(newTX));
            Log.e("TX_HASH_1", BTCUtils.toHex(hashData));
            Log.e("TX_HASH_2", BTCUtils.toHex(doubleHashData));

            unspentOutputs.get(i).bodyDoubleHash = doubleHashData;
            unspentOutputs.get(i).bodyHash = hashData;

            hashesForSign[i] = doubleHashData;
        }

        byte[] signFromCard = new byte[64 * unspentOutputs.size()];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0 + i * 64, 32 + i * 64));
            BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
            byte[] encodingSign = DerEncodingUtil.packSignDer(r, s, pbKey);
            unspentOutputs.get(i).scriptForBuild = encodingSign;
        }

        byte[] realTX = BTCUtils.buildTXForSend(outputAddress, changeAddress, unspentOutputs, amount, change);

        return realTX.length;
    }

    public void doSetFee(int checkedRadioButtonId) {
        switch (checkedRadioButtonId) {
            case R.id.rbMinimalFee:
                if (minFee != null) etFee.setText(minFee);
                else etFee.setText("?");
                break;
            case R.id.rbNormalFee:
                if (normalFee != null) etFee.setText(normalFee);
                else etFee.setText("?");
                break;
            case R.id.rbMaximumFee:
                if (maxFee != null) etFee.setText(maxFee);
                else etFee.setText("?");
                break;
        }
    }

}