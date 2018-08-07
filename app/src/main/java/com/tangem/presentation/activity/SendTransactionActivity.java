package com.tangem.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.request.InfuraRequest;
import com.tangem.data.network.task.send_transaction.ConnectTask;
import com.tangem.data.network.task.send_transaction.ETHRequestTask;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.wallet.R;

public class SendTransactionActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    public TangemCard mCard;
    private String tx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_transaction);

        MainActivity.Companion.commonInit(getApplicationContext());

        progressBar = findViewById(R.id.progressBar);

        Intent intent = getIntent();
        mCard = new TangemCard(getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(intent.getExtras().getBundle("Card"));
        tx = intent.getStringExtra("TX");

        CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
        if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet || mCard.getBlockchain() == Blockchain.Token) {
            ETHRequestTask task = new ETHRequestTask(SendTransactionActivity.this, mCard.getBlockchain());
            InfuraRequest req = InfuraRequest.SendTransaction(mCard.getWallet(), tx);
            req.setID(67);
            req.setBlockchain(mCard.getBlockchain());
            task.execute(req);
        } else if (mCard.getBlockchain() == Blockchain.Bitcoin || mCard.getBlockchain() == Blockchain.BitcoinTestNet) {
            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);
            ConnectTask connectTask = new ConnectTask(SendTransactionActivity.this, nodeAddress, nodePort,3);
            connectTask.execute(ElectrumRequest.Broadcast(mCard.getWallet(), tx));
        } else if (mCard.getBlockchain() == Blockchain.BitcoinCash || mCard.getBlockchain() == Blockchain.BitcoinCashTestNet) {
            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);
            ConnectTask connectTask = new ConnectTask(SendTransactionActivity.this, nodeAddress, nodePort,3);
            connectTask.execute(ElectrumRequest.Broadcast(mCard.getWallet(), tx));
        }

    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_BACK:
                Toast.makeText(this, R.string.please_wait, Toast.LENGTH_LONG).show();
                return true;
        }
        return super.onKeyDown(keycode, e);
    }

    public void finishWithError(String Message) {
        Intent intent = new Intent();
        intent.putExtra("message", "Try again. Failed to send transaction (" + Message + ")");
        setResult(MainActivity.RESULT_CANCELED, intent);
        finish();
    }

    public void finishWithSuccess() {
        Intent intent = new Intent();
        intent.putExtra("message", getString(R.string.transaction_has_been_successfully_signed));
        setResult(MainActivity.RESULT_OK, intent);
        finish();
    }

}
