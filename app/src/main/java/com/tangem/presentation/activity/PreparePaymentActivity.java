package com.tangem.presentation.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.util.FormatUtil;
import com.tangem.wallet.R;

import java.io.IOException;

public class PreparePaymentActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    public static final String TAG = PreparePaymentActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SCAN_QR = 1;
    private static final int REQUEST_CODE_SEND_PAYMENT = 2;
    private EditText etWallet;
    private EditText etAmount;
    //    private TextView tvAmountEquivalent;
    boolean use_mCurrency;
    private TangemCard mCard;
    private NfcManager mNfcManager;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prepare_payment);

        MainActivity.commonInit(getApplicationContext());

        mNfcManager = new NfcManager(this, this);

        mCard = new TangemCard(getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(getIntent().getExtras().getBundle("Card"));

        Button btnVerify = findViewById(R.id.btnVerify);
        etWallet = findViewById(R.id.etWallet);
        etAmount = findViewById(R.id.etAmount);
        ImageView ivCamera = findViewById(R.id.ivCamera);
        TextView tvCurrency = findViewById(R.id.tvCurrency);
        TextView tvCardId = findViewById(R.id.tvCardID);
        TextView tvBalance = findViewById(R.id.tvBalance);
//        TextView tvBalanceEquivalent = findViewById(R.id.tvBalanceEquivalent);
//        tvAmountEquivalent = findViewById(R.id.tvAmountEquivalent);

        tvCardId.setText(mCard.getCIDDescription());
        CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());

        if (mCard.getBlockchain() == Blockchain.Token) {
            Spanned html = Html.fromHtml(engine.GetBalanceWithAlter(mCard));
            tvBalance.setText(html);
        } else {
            tvBalance.setText(engine.GetBalanceWithAlter(mCard));
        }

//        tvBalanceEquivalent.setText(engine.GetBalanceEquivalent(mCard));

        if (etAmount != null && mCard.getRemainingSignatures() < 2) {
            etAmount.setEnabled(false);
        }

//        if (!mCard.getAmountEquivalentDescriptionAvailable()) {
//            tvBalanceEquivalent.setError("Service unavailable");
//        } else {
//            tvBalanceEquivalent.setError(null);
//        }

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                try {
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

        if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet) {
            tvCurrency.setText(engine.GetBalanceCurrency(mCard));
            use_mCurrency = false;
            etAmount.setText(engine.GetBalanceValue(mCard));
        } else if (mCard.getBlockchain() == Blockchain.Bitcoin || mCard.getBlockchain() == Blockchain.BitcoinTestNet) {
            Double balance = engine.GetBalanceLong(mCard) / (mCard.getBlockchain().getMultiplier() / 1000.0);
            tvCurrency.setText("m" + mCard.getBlockchain().getCurrency());
            use_mCurrency = true;
            String output = FormatUtil.DoubleToString(balance);
            etAmount.setText(output);
        } else if (mCard.getBlockchain() == Blockchain.BitcoinCash || mCard.getBlockchain() == Blockchain.BitcoinCashTestNet) {
            Double balance = engine.GetBalanceLong(mCard) / (mCard.getBlockchain().getMultiplier() / 1000.0);
            tvCurrency.setText("m" + mCard.getBlockchain().getCurrency());
            use_mCurrency = true;
            String output = FormatUtil.DoubleToString(balance);
            etAmount.setText(output);
        } else {
            tvCurrency.setText(engine.GetBalanceCurrency(mCard));
            use_mCurrency = false;
            etAmount.setText(engine.GetBalanceValue(mCard));
        }

        btnVerify.setOnClickListener(v -> {
            String strAmount;
            strAmount = etAmount.getText().toString().replace(",",".");
            CoinEngine engine1 = CoinEngineFactory.Create(mCard.getBlockchain());

            Log.i(TAG, mCard.getBlockchain().getOfficialName());

            try {
                if (!engine.CheckAmount(mCard, etAmount.getText().toString())) {
                    etAmount.setError(getString(R.string.not_enough_funds_on_your_card));
                }
            } catch (Exception e) {
                etAmount.setError(getString(R.string.unknown_amount_format));
                return;
            }

            boolean checkAddress = engine1.ValdateAddress(etWallet.getText().toString(), mCard);
            if (!checkAddress) {
                etWallet.setError(getString(R.string.incorrect_destination_wallet_address));
                return;
            }

            if (etWallet.getText().toString().equals(mCard.getWallet())) {
                etWallet.setError(getString(R.string.destination_wallet_address_equal_source_address));
                return;
            }

            Intent intent = new Intent(getBaseContext(), ConfirmPaymentActivity.class);
            intent.putExtra("UID", mCard.getUID());
            intent.putExtra("Card", mCard.getAsBundle());
            intent.putExtra("Wallet", etWallet.getText().toString());
            intent.putExtra("Amount", strAmount);
            startActivityForResult(intent, REQUEST_CODE_SEND_PAYMENT);
        });

        ivCamera.setOnClickListener(v -> {
            Intent intent = new Intent(getBaseContext(), QRScanActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SCAN_QR);
        });
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
        if (requestCode == REQUEST_CODE_SCAN_QR && resultCode == Activity.RESULT_OK && data != null && data.getExtras().containsKey("QRCode")) {
            String code = data.getStringExtra("QRCode");
            if (code.contains("bitcoin:")) {
                String tmp[] = code.split("bitcoin:");
                code = tmp[1];
            }
            etWallet.setText(code);
        } else if (requestCode == REQUEST_CODE_SEND_PAYMENT) {

            setResult(resultCode, data);
            finish();
        }
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

}