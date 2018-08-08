package com.tangem.data.network.task.send_transaction;

import android.util.Log;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.task.ElectrumTask;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.domain.wallet.SharedData;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.presentation.activity.SendTransactionActivity;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.List;

public class ConnectTask extends ElectrumTask {
    private WeakReference<SendTransactionActivity> reference;
    private int remaining_attempts;


    private void CreateChildTask(TangemCard mCard, String tx, String error_message) {

        if (remaining_attempts > 0) {

            remaining_attempts--;

            CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());

            if (mCard.getBlockchain() == Blockchain.Bitcoin || mCard.getBlockchain() == Blockchain.BitcoinTestNet) {
                String nodeAddress = engine.GetNode(mCard);
                int nodePort = engine.GetNodePort(mCard);
                ConnectTask connectTask = new ConnectTask(reference.get(), nodeAddress, nodePort, remaining_attempts);
                connectTask.execute(ElectrumRequest.Broadcast(mCard.getWallet(), tx));
            } else if (mCard.getBlockchain() == Blockchain.BitcoinCash || mCard.getBlockchain() == Blockchain.BitcoinCashTestNet) {
                String nodeAddress = engine.GetNode(mCard);
                int nodePort = engine.GetNodePort(mCard);
                ConnectTask connectTask = new ConnectTask(reference.get(), nodeAddress, nodePort, remaining_attempts);
                connectTask.execute(ElectrumRequest.Broadcast(mCard.getWallet(), tx));
            }

        } else {
            reference.get().finishWithError(error_message);
        }
    }


    public ConnectTask(SendTransactionActivity context, String host, int port, int attempts) {
        super(host, port);
        reference = new WeakReference<>(context);
        remaining_attempts = attempts;
    }

    public ConnectTask(SendTransactionActivity context, String host, int port, int attempts, SharedData sharedData) {
        super(host, port, sharedData);
        reference = new WeakReference<>(context);
        remaining_attempts = attempts;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(List<ElectrumRequest> requests) {
        super.onPostExecute(requests);
        SendTransactionActivity sendTransactionActivity = reference.get();

        CoinEngine engine = CoinEngineFactory.Create(Blockchain.Bitcoin);

        for (ElectrumRequest request : requests) {
            try {
                if (request.error == null) {
                    if (request.isMethod(ElectrumRequest.METHOD_SendTransaction)) {
                        try {
                            String hashTX = request.getResultString();

                            try {
                                LastSignStorage.setLastMessage(sendTransactionActivity.getCard().getWallet(), hashTX);
                                if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                    hashTX = hashTX.substring(2);
                                }
                                BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                                LastSignStorage.setTxWasSend(sendTransactionActivity.getCard().getWallet());
                                LastSignStorage.setLastMessage(sendTransactionActivity.getCard().getWallet(), "");
                                Log.e("TX_RESULT", hashTX);
                                sendTransactionActivity.finishWithSuccess();
                            } catch (Exception e) {
                                engine.SwitchNode(null);
//                                sendTransactionActivity.finishWithError(hashTX);
                                CreateChildTask(sendTransactionActivity.getCard(), request.TX, hashTX);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            engine.SwitchNode(null);
//                            sendTransactionActivity.finishWithError(e.toString());
                            CreateChildTask(sendTransactionActivity.getCard(), request.TX, e.toString());
                        }
                    }
                } else {
                    engine.SwitchNode(null);
//                    sendTransactionActivity.finishWithError(request.error);
                    CreateChildTask(sendTransactionActivity.getCard(), request.TX, request.error);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                engine.SwitchNode(null);
//                sendTransactionActivity.finishWithError(e.toString());
                CreateChildTask(sendTransactionActivity.getCard(), request.TX, e.toString());
            }
        }
    }

}