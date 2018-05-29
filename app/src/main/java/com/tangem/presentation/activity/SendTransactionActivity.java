package com.tangem.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.task.ElectrumTask;
import com.tangem.domain.wallet.Infura_Request;
import com.tangem.data.network.task.InfuraTask;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.wallet.R;
import com.tangem.domain.wallet.SharedData;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.List;

public class SendTransactionActivity extends AppCompatActivity {

    ProgressBar progressBar;
    private TangemCard mCard;
    private String tx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_transaction);

        MainActivity.commonInit(getApplicationContext());

        progressBar = findViewById(R.id.progressBar);

        Intent intent = getIntent();
        mCard = new TangemCard(getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(intent.getExtras().getBundle("Card"));
        tx = intent.getStringExtra("TX");

        CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
        if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet || mCard.getBlockchain() == Blockchain.Token) {
            ETHRequestTask task = new ETHRequestTask(mCard.getBlockchain());
            Infura_Request req = Infura_Request.SendTransaction(mCard.getWallet(), tx);
            req.setID(67);
            req.setBlockchain(mCard.getBlockchain());
            task.execute(req);
        } else if (mCard.getBlockchain() == Blockchain.Bitcoin || mCard.getBlockchain() == Blockchain.BitcoinTestNet ) {
            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);
            ConnectTask connectTask = new ConnectTask(nodeAddress, nodePort);
            connectTask.execute(ElectrumRequest.Broadcast(mCard.getWallet(), tx));
        }
        else if (mCard.getBlockchain() == Blockchain.BitcoinCash || mCard.getBlockchain() == Blockchain.BitcoinCashTestNet ) {
            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);
            ConnectTask connectTask = new ConnectTask(nodeAddress, nodePort);
            connectTask.execute(ElectrumRequest.Broadcast(mCard.getWallet(), tx));
        }

    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_BACK:
                Toast.makeText(getBaseContext(),"Please wait while the payment is sent...",Toast.LENGTH_LONG).show();
                return true;
        }

        return super.onKeyDown(keycode, e);
    }

    void FinishWithError(String Message) {
        Intent intent = new Intent();
        intent.putExtra("message", "Failed to send transaction. Try again.");
        setResult(MainActivity.RESULT_CANCELED, intent);
        finish();
    }

    void FinishWithSuccess() {
        Intent intent = new Intent();
        intent.putExtra("message", "Transaction has been successfully signed and sent to blockchain node. Wallet balance will be updated in a while");
        setResult(MainActivity.RESULT_OK, intent);
        finish();
    }

    private class ETHRequestTask extends InfuraTask {
        ETHRequestTask(Blockchain blockchain){
            super(blockchain);
        }
        @Override
        protected void onPostExecute(List<Infura_Request> requests) {
            super.onPostExecute(requests);
            for (Infura_Request request : requests) {
                try {
                    if (request.error == null) {
                        if (request.isMethod(Infura_Request.METHOD_ETH_SendRawTransaction)) {
                            try {
                                String hashTX = "";
                                try {
                                    String tmp = request.getResultString();
                                    hashTX = tmp;
                                }catch(JSONException e)
                                {
                                    JSONObject msg = request.getAnswer();
                                    JSONObject err = msg.getJSONObject("error");
                                    hashTX = err.getString("message");
                                    LastSignStorage.setLastMessage(mCard.getWallet(), hashTX);
                                    FinishWithError(hashTX);
                                    return;
                                }

                                try {
                                    if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                        hashTX = hashTX.substring(2);
                                    }
                                    BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                                    LastSignStorage.setTxWasSend(mCard.getWallet());
                                    LastSignStorage.setLastMessage(mCard.getWallet(), "");
                                    BigInteger nonce = mCard.GetConfirmTXCount();
                                    nonce.add(BigInteger.valueOf(1));
                                    mCard.SetConfirmTXCount(nonce);
                                    Log.e("TX_RESULT", hashTX);
                                    FinishWithSuccess();
                                }catch(Exception e)
                                {
                                    FinishWithError(hashTX);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                FinishWithError(e.toString());
                            }
                        }
                    } else if (request.error != null) {
                        FinishWithError(request.error);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    FinishWithError(e.toString());
                }
            }
        }
    }

    private class ConnectTask extends ElectrumTask {
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
        protected void onPostExecute(List<ElectrumRequest> requests) {
            super.onPostExecute(requests);
            CoinEngine engine = CoinEngineFactory.Create(Blockchain.Bitcoin);

            for (ElectrumRequest request : requests) {
                try {
                    if (request.error == null) {
                        if (request.isMethod(ElectrumRequest.METHOD_SendTransaction)) {
                            try {
                                String hashTX = request.getResultString();

                                try
                                {
                                    LastSignStorage.setLastMessage(mCard.getWallet(), hashTX);
                                    if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                        hashTX = hashTX.substring(2);
                                    }
                                    BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                                    LastSignStorage.setTxWasSend(mCard.getWallet());
                                    LastSignStorage.setLastMessage(mCard.getWallet(), "");
                                    Log.e("TX_RESULT", hashTX);
                                    FinishWithSuccess();
                                }catch(Exception e)
                                {
                                    engine.SwitchNode(null);
                                    FinishWithError(hashTX);
                                    return;
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.SwitchNode(null);
                                FinishWithError(e.toString());
                            }
                        }
                    } else if (request.error != null) {
                        engine.SwitchNode(null);
                        FinishWithError(request.error);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    engine.SwitchNode(null);
                    FinishWithError(e.toString());
                }
            }

        }
    }

}
