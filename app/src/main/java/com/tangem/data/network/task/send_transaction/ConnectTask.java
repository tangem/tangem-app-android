package com.tangem.data.network.task.send_transaction;

import android.util.Log;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.task.ElectrumTask;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.domain.wallet.SharedData;
import com.tangem.presentation.activity.SendTransactionActivity;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.List;

public class ConnectTask extends ElectrumTask {
    private WeakReference<SendTransactionActivity> reference;

    public ConnectTask(SendTransactionActivity context, String host, int port) {
        super(host, port);
        reference = new WeakReference<>(context);
    }

    public ConnectTask(SendTransactionActivity context, String host, int port, SharedData sharedData) {
        super(host, port, sharedData);
        reference = new WeakReference<>(context);
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
                                LastSignStorage.setLastMessage(sendTransactionActivity.mCard.getWallet(), hashTX);
                                if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                    hashTX = hashTX.substring(2);
                                }
                                BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                                LastSignStorage.setTxWasSend(sendTransactionActivity.mCard.getWallet());
                                LastSignStorage.setLastMessage(sendTransactionActivity.mCard.getWallet(), "");
                                Log.e("TX_RESULT", hashTX);
                                sendTransactionActivity.finishWithSuccess();
                            } catch (Exception e) {
                                engine.SwitchNode(null);
                                sendTransactionActivity.finishWithError(hashTX);
                                return;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            engine.SwitchNode(null);
                            sendTransactionActivity.finishWithError(e.toString());
                        }
                    }
                } else if (request.error != null) {
                    engine.SwitchNode(null);
                    sendTransactionActivity.finishWithError(request.error);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                engine.SwitchNode(null);
                sendTransactionActivity.finishWithError(e.toString());
            }
        }
    }

}