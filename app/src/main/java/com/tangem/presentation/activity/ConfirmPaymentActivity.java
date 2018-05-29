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

import com.tangem.cardReader.NfcManager;
import com.tangem.cardReader.Util;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.Blockchain;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.CoinEngineFactory;
import com.tangem.wallet.DerEncodingUtil;
import com.tangem.wallet.Electrum_Request;
import com.tangem.wallet.Electrum_Task;
import com.tangem.wallet.Fee_Request;
import com.tangem.wallet.Fee_Task;
import com.tangem.wallet.FormatUtil;
import com.tangem.wallet.Infura_Request;
import com.tangem.wallet.Infura_Task;
import com.tangem.wallet.R;
import com.tangem.wallet.SharedData;
import com.tangem.wallet.Tangem_Card;
import com.tangem.wallet.Transaction;
import com.tangem.wallet.UnspentOutputInfo;

import org.json.JSONException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ConfirmPaymentActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final int REQUEST_CODE_SIGN_PAYMENT = 1;
    private static final int REQUEST_CODE_REQUEST_PIN2 = 2;
    Button btnSend;
    boolean feeRequestSuccess = false;
    boolean balanceRequestSuccess = false;
    EditText etWallet;
    TextView tvCardID, tvBalance, tvCurrency, tvCurrency2, tvBalanceEquivalent, tvAmountEquivalent, tvFeeEquivalent;
    EditText etAmount;
    EditText etFee;
    ImageView ivCamera;
    Tangem_Card mCard;
    RadioGroup rgFee;
    String minFee = null, maxFee = null, normalFee = null;
    Long minFeeInInternalUnits = 0L;
    private NfcManager mNfcManager;
    int requestPIN2Count = 0;
    ProgressBar progressBar;
    boolean nodeCheck = false;
    Date dtVerifyed = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_payment);

        MainActivity.commonInit(getApplicationContext());
        mNfcManager = new NfcManager(this, this);

        mCard = new Tangem_Card(getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(getIntent().getExtras().getBundle("Card"));

        progressBar = findViewById(R.id.progressBar);

        btnSend = findViewById(R.id.btnSend);
        etWallet = findViewById(R.id.etWallet);
        tvCardID = findViewById(R.id.tvCardID);
        tvBalance = findViewById(R.id.tvBalance);
        tvCurrency = findViewById(R.id.tvCurrency);
        tvCurrency2 = findViewById(R.id.tvCurrency2);
        etAmount = findViewById(R.id.etAmount);
        etFee = findViewById(R.id.etFee);
        tvBalanceEquivalent = findViewById(R.id.tvBalanceEquivalent);
        tvAmountEquivalent = findViewById(R.id.tvAmountEquivalent);

        tvFeeEquivalent = findViewById(R.id.tvFeeEquivalent);
        ivCamera = findViewById(R.id.ivCamera);

        rgFee = findViewById(R.id.rgFee);
        rgFee.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                doSetFee(checkedId);
            }
        });

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {

                    CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
                    tvAmountEquivalent.setText(engine.GetAmountEqualentDescriptor(mCard, etAmount.getText().toString()));
                    if (!mCard.getAmountEquivalentDescriptionAvailable()) {
                        tvAmountEquivalent.setError("Service unavailable");
                    } else {
                        tvAmountEquivalent.setError(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    tvAmountEquivalent.setText("");
                }
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
        etAmount.setText(getIntent().getStringExtra("Amount"));
        tvCurrency.setText(engine.GetBalanceCurrency(mCard));
        tvCurrency2.setText(engine.GetFeeCurrency());

        tvCardID.setText(mCard.getCIDDescription());

        //tvBalanceEquivalent.setText(mCard.getBalanceEquivalentDescription());
        tvBalanceEquivalent.setText(engine.GetBalanceEquivalent(mCard));
        if (!mCard.getAmountEquivalentDescriptionAvailable()) {
            tvBalanceEquivalent.setError("Service unavailable");
        } else {
            tvBalanceEquivalent.setError(null);
        }

        etWallet.setText(getIntent().getStringExtra("Wallet"));

        etFee.setText("?");

        btnSend.setVisibility(View.INVISIBLE);
        feeRequestSuccess = false;
        balanceRequestSuccess = false;
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, -1);

                if (dtVerifyed == null || dtVerifyed.before(calendar.getTime())) {
                    FinishActivityWithError(Activity.RESULT_CANCELED, "The obtained data is outdated! Try again");
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
                    FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot check balance! No connection with blockchain nodes");
                    return;
                } else if (!engineCoin.IsBalanceNotZero(mCard)) {
                    FinishActivityWithError(Activity.RESULT_CANCELED, "The wallet is empty");
                    return;
                } else if (!engineCoin.CheckUnspentTransaction(mCard)) {
                    //else if (mCard.getUnspentTransactions().size() == 0 && mCard.getBlockchain() != Blockchain.Ethereum) {
                    FinishActivityWithError(Activity.RESULT_CANCELED, "Please wait for confirmation of incoming transaction");
                    return;
                }

                if (!engineCoin.CheckAmountValie(mCard, txAmount, txFee, minFeeInInternalUnits)) {
                    FinishActivityWithError(Activity.RESULT_CANCELED, "Fee exceeds payment amount. Enter correct value and repeat sending.");
                    return;
                }

                requestPIN2Count = 0;
                Intent intent = new Intent(getBaseContext(), RequestPINActivity.class);
                intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                intent.putExtra("UID", mCard.getUID());
                intent.putExtra("Card", mCard.getAsBundle());
                startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2);
            }
        });

        if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet || mCard.getBlockchain() == Blockchain.Token) {
            ETHRequestTask task = new ETHRequestTask(mCard.getBlockchain());
            Infura_Request req = Infura_Request.GetGasPrise(mCard.getWallet());
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
                //ConnectTask connectTaskEx = new ConnectTask(Blockchain.getNextServiceHost(mCard), Blockchain.getNextServicePort(mCard), data);
                ConnectTask connectTaskEx = new ConnectTask(nodeAddress, nodePort, data);

                //connectTaskEx.execute(Electrum_Request.CheckBalance(mCard.getWallet()));
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, Electrum_Request.CheckBalance(mCard.getWallet()));
            }

            String nodeAddress = engineCoin.GetNode(mCard);
            int nodePort = engineCoin.GetNodePort(mCard);
            ConnectTask connectTask = new ConnectTask(nodeAddress, nodePort, data);

            //ConnectTask connectTask = new ConnectTask(Blockchain.getServiceHost(mCard), Blockchain.getServicePort(mCard));

            connectTask.execute(/*Electrum_Request.CheckBalance(mCard.getWallet()), */Electrum_Request.GetFee(mCard.getWallet()));

            int calcSize = 256;
            try {
                calcSize = BuildSize(etWallet.getText().toString(), "0.00", etAmount.getText().toString());
            } catch (Exception ex) {
                Log.e("Build Fee error", ex.getMessage());
            }

            SharedData sharedFee = new SharedData(SharedData.COUNT_REQUEST);

            progressBar.setVisibility(View.VISIBLE);
            for (int i = 0; i < SharedData.COUNT_REQUEST; ++i) {

                ConnectFeeTask feeTask = new ConnectFeeTask(sharedFee);

                feeTask.execute(Fee_Request.GetFee(mCard.getWallet(), calcSize, Fee_Request.NORMAL),
                        Fee_Request.GetFee(mCard.getWallet(), calcSize, Fee_Request.MINIMAL),
                        Fee_Request.GetFee(mCard.getWallet(), calcSize, Fee_Request.PRIORITY));
            }
        }

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Intent intent = new Intent();
                intent.putExtra("message", "Operation canceled");
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_PAYMENT) {
            if (data != null && data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
                Tangem_Card updatedCard = new Tangem_Card(data.getStringExtra("UID"));
                updatedCard.LoadFromBundle(data.getBundleExtra("Card"));
                mCard = updatedCard;
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
                intent.putExtra("Amount", etAmount.getText().toString());
                intent.putExtra("Fee", etFee.getText().toString());
                startActivityForResult(intent, REQUEST_CODE_SIGN_PAYMENT);
            } else {
                Toast.makeText(getBaseContext(), "PIN2 is required to sign the payment", Toast.LENGTH_LONG).show();
            }
        }
    }

    void FinishActivityWithError(int errorCode, String message) {
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
            mNfcManager.IgnoreTag(tag);
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
        List<Tangem_Card.UnspentTransaction> rawTxList = mCard.getUnspentTransactions();
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

    private class ConnectTask extends Electrum_Task {
        public ConnectTask(String host, int port) {
            super(host, port);
        }

        public ConnectTask(String host, int port, SharedData sharedData) {
            super(host, port, sharedData);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(List<Electrum_Request> requests) {
            super.onPostExecute(requests);
            for (Electrum_Request request : requests) {
                try {
                    if (request.error == null) {
                        if (request.isMethod(Electrum_Request.METHOD_GetBalance)) {
                            try {
                                etFee.setText("--");

                                //String mWalletAddress = request.getParams().getString(0);
                                if ((request.getResult().getInt("confirmed") + request.getResult().getInt("unconfirmed")) / mCard.getBlockchain().getMultiplier() * 1000000.0 < Float.parseFloat(etAmount.getText().toString())) {
                                    etFee.setError("Not enough funds");
                                    balanceRequestSuccess = false;
                                    btnSend.setVisibility(View.INVISIBLE);
                                    dtVerifyed = null;
                                    nodeCheck = false;
                                } else {
                                    etFee.setError(null);
                                    balanceRequestSuccess = true;
                                    if (feeRequestSuccess && balanceRequestSuccess) {
                                        btnSend.setVisibility(View.VISIBLE);

                                    }
                                    dtVerifyed = new Date();
                                    nodeCheck = true;
                                }
                            } catch (JSONException e) {
                                if (sharedCounter != null) {
                                    int errCounter = sharedCounter.errorRequest.incrementAndGet();
                                    if (errCounter >= sharedCounter.allRequest) {
                                        e.printStackTrace();
                                        FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot check balance! No connection with blockchain nodes");
                                    }
                                } else {
                                    e.printStackTrace();
                                    FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot check balance! No connection with blockchain nodes");
                                }
                            }
                        } else if (request.isMethod(Electrum_Request.METHOD_GetFee)) {
                            if (request.getResultString() == "-1") {
                                etFee.setText("3");
                            }
                        }
                    } else {
//                        etFee.setError(request.error);
//                        btnSend.setVisibility(View.INVISIBLE);
                        if (sharedCounter != null) {
                            int errCounter = sharedCounter.errorRequest.incrementAndGet();
                            if (errCounter >= sharedCounter.allRequest) {
                                FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                            }
                        } else {
                            FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                        }
                        return;
                    }
                } catch (JSONException e) {
                    if (sharedCounter != null) {
                        int errCounter = sharedCounter.errorRequest.incrementAndGet();
                        if (errCounter >= sharedCounter.allRequest) {
                            e.printStackTrace();
                            FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");

                        }
                    } else {
                        e.printStackTrace();
                        FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                    }
                }
            }

        }
    }

    private class ETHRequestTask extends Infura_Task {
        ETHRequestTask(Blockchain blockchain) {
            super(blockchain);
        }

        @Override
        protected void onPostExecute(List<Infura_Request> requests) {
            super.onPostExecute(requests);
            for (Infura_Request request : requests) {
                try {
                    Long price = 0L;
                    if (request.error == null) {

                        if (request.isMethod(Infura_Request.METHOD_ETH_GetGasPrice)) {
                            try {
                                String gasPrice = request.getResultString();
                                gasPrice = gasPrice.substring(2);
                                BigInteger l = new BigInteger(gasPrice, 16);

                                BigInteger m = mCard.getBlockchain() == Blockchain.Token ? BigInteger.valueOf(55000) : BigInteger.valueOf(21000);
                                l = l.multiply(m);
                                String feeInGwei = mCard.getAmountInGwei(String.valueOf(l));

                                minFee = feeInGwei;
                                maxFee = feeInGwei;
                                normalFee = feeInGwei;
                                etFee.setText(feeInGwei);
                                etFee.setError(null);
                                btnSend.setVisibility(View.VISIBLE);
                                feeRequestSuccess = true;
                                balanceRequestSuccess = true;

                                dtVerifyed = new Date();
                                minFeeInInternalUnits = mCard.InternalUnitsFromString(feeInGwei);

                            } catch (JSONException e) {
                                e.printStackTrace();
                                FinishActivityWithError(Activity.RESULT_CANCELED, "Can't calculate fee! No connection with blockchain nodes");
                            }
                        }
                    } else {
                        FinishActivityWithError(Activity.RESULT_CANCELED, "Can't calculate fee! No connection with blockchain nodes");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    FinishActivityWithError(Activity.RESULT_CANCELED, "Can't calculate fee! No connection with blockchain nodes");
                }
            }
        }
    }

    private class ConnectFeeTask extends Fee_Task {
        public ConnectFeeTask(SharedData sharedData) {
            super(sharedData);
        }

        @Override
        protected void onPostExecute(List<Fee_Request> requests) {
            super.onPostExecute(requests);
            for (Fee_Request request : requests) {
                if (request.error == null) {
                    long minFeeRate = 0;

                    try {

                        try {
                            String tmpAnswer = request.getAsString();
                            BigDecimal minFeeBD = new BigDecimal(tmpAnswer);
                            BigDecimal multiplicator = new BigDecimal("100000000");
                            minFeeBD = minFeeBD.multiply(multiplicator);
                            BigInteger minFeeBI = minFeeBD.toBigInteger();
                            minFeeRate = minFeeBI.longValue();
                        } catch (Exception e) {

                            if (sharedCounter != null) {
                                int errCounter = sharedCounter.errorRequest.incrementAndGet();


                                if (errCounter >= sharedCounter.allRequest) {
                                    progressBar.setVisibility(View.INVISIBLE);
                                    FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                                }
                            } else {
                                progressBar.setVisibility(View.INVISIBLE);
                                FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                            }

                            //FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                            return;
                        }

                        if (minFeeRate == 0) {
                            progressBar.setVisibility(View.INVISIBLE);
                            FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! Wrong data received from the node");
                            return;
                        }

                        long inputCount = request.txSize;

                        if (inputCount != 0) {
                            minFeeRate = minFeeRate * inputCount;
                        } else {
                            minFeeRate = minFeeRate * 256;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (sharedCounter != null) {
                            int errCounter = sharedCounter.errorRequest.incrementAndGet();
                            if (errCounter >= sharedCounter.allRequest) {
                                progressBar.setVisibility(View.INVISIBLE);
                                FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                            }
                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                        }
                        return;
                    }

                    progressBar.setVisibility(View.INVISIBLE);

                    float finalFee = (float) minFeeRate / (float) 10000;

                    finalFee = Math.round(finalFee) / (float) 10000;

                    if (request.getBlockCount() == Fee_Request.MINIMAL) {
                        minFee = String.valueOf(finalFee);
                        minFeeInInternalUnits = mCard.InternalUnitsFromString(String.valueOf(finalFee));
                    } else if (request.getBlockCount() == Fee_Request.NORMAL) {
                        normalFee = String.valueOf(finalFee);
                    } else if (request.getBlockCount() == Fee_Request.PRIORITY) {
                        maxFee = String.valueOf(finalFee);
                    }

                    doSetFee(rgFee.getCheckedRadioButtonId());

                    etFee.setError(null);
                    feeRequestSuccess = true;
                    if (feeRequestSuccess && balanceRequestSuccess) {
                        btnSend.setVisibility(View.VISIBLE);
                    }
                    dtVerifyed = new Date();

                } else {

                    if (sharedCounter != null) {
                        int errCounter = sharedCounter.errorRequest.incrementAndGet();
                        if (errCounter >= sharedCounter.allRequest) {
                            progressBar.setVisibility(View.INVISIBLE);
                            FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                        }
                    } else {
                        progressBar.setVisibility(View.INVISIBLE);
                        FinishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                    }
                }
            }
        }
    }

    private void doSetFee(int checkedRadioButtonId) {
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
