package com.tangem.data.network.task.send_transaction;

import android.util.Log;

import com.tangem.data.network.request.InfuraRequest;
import com.tangem.data.network.task.InfuraTask;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.presentation.activity.SendTransactionActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.List;

public class ETHRequestTask extends InfuraTask {
    private WeakReference<SendTransactionActivity> reference;

    public ETHRequestTask(SendTransactionActivity context, Blockchain blockchain) {
        super(blockchain);
        reference = new WeakReference<>(context);
    }

    @Override
    protected void onPostExecute(List<InfuraRequest> requests) {
        super.onPostExecute(requests);
        SendTransactionActivity sendTransactionActivity = reference.get();

        for (InfuraRequest request : requests) {
            try {
                if (request.error == null) {
                    if (request.isMethod(InfuraRequest.METHOD_ETH_SendRawTransaction)) {
                        try {
                            String hashTX = "";
                            try {
                                String tmp = request.getResultString();
                                hashTX = tmp;
                            } catch (JSONException e) {
                                JSONObject msg = request.getAnswer();
                                JSONObject err = msg.getJSONObject("error");
                                hashTX = err.getString("message");
                                LastSignStorage.setLastMessage(sendTransactionActivity.getCard().getWallet(), hashTX);
                                Log.e("Send_TX_Error:", hashTX);
                                sendTransactionActivity.finishWithError(hashTX);
                                return;
                            }

                            try {
                                if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                    hashTX = hashTX.substring(2);
                                }
                                BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                                LastSignStorage.setTxWasSend(sendTransactionActivity.getCard().getWallet());
                                LastSignStorage.setLastMessage(sendTransactionActivity.getCard().getWallet(), "");
                                BigInteger nonce = sendTransactionActivity.getCard().GetConfirmTXCount();
                                nonce.add(BigInteger.valueOf(1));
                                sendTransactionActivity.getCard().SetConfirmTXCount(nonce);
                                Log.e("TX_RESULT", hashTX);
                                sendTransactionActivity.finishWithSuccess();
                            } catch (Exception e) {
                                sendTransactionActivity.finishWithError(hashTX);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            sendTransactionActivity.finishWithError(e.toString());
                        }
                    }
                } else if (request.error != null) {
                    sendTransactionActivity.finishWithError(request.error);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                sendTransactionActivity.finishWithError(e.toString());
            }
        }
    }

}