package com.tangem.data.network.task.loaded_wallet;

import android.util.Log;

import com.tangem.data.network.request.InfuraRequest;
import com.tangem.data.network.task.InfuraTask;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.presentation.fragment.LoadedWallet;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.List;

public class ETHRequestTask extends InfuraTask {
    private WeakReference<LoadedWallet> reference;

    public ETHRequestTask(LoadedWallet context, Blockchain blockchain) {
        super(blockchain);
        reference = new WeakReference<>(context);
    }

    @Override
    protected void onPostExecute(List<InfuraRequest> requests) {
        super.onPostExecute(requests);
        LoadedWallet loadedWallet = reference.get();

        for (InfuraRequest request : requests) {
            try {
                if (request.error == null) {

                    if (request.isMethod(InfuraRequest.METHOD_ETH_GetBalance)) {
                        try {
                            String balanceCap = request.getResultString();
                            balanceCap = balanceCap.substring(2);
                            BigInteger l = new BigInteger(balanceCap, 16);
                            BigInteger d = l.divide(new BigInteger("1000000000000000000", 10));
                            Long balance = d.longValue();

                            loadedWallet.mCard.setBalanceConfirmed(balance);
                            loadedWallet.mCard.setBalanceUnconfirmed(0L);
                            if (loadedWallet.mCard.getBlockchain() != Blockchain.Token)
                                loadedWallet.mCard.setDecimalBalance(l.toString(10));
                            loadedWallet.mCard.setDecimalBalanceAlter(l.toString(10));

                        } catch (JSONException e) {
                            e.printStackTrace();
                            loadedWallet.errorOnUpdate(e.toString());
                        }
                    } else if (request.isMethod(InfuraRequest.METHOD_ETH_Call)) {
                        try {
                            String balanceCap = request.getResultString();
                            balanceCap = balanceCap.substring(2);
                            BigInteger l = new BigInteger(balanceCap, 16);
                            Long balance = l.longValue();

//                                Log.i(TAG, " dvddvdv  BigInteger.ZERO");

                            if (l.compareTo(BigInteger.ZERO) == 0) {

//                                    Log.i(TAG, "BigInteger.ZERO");

                                loadedWallet.mCard.setBlockchainID(Blockchain.Ethereum.getID());
                                loadedWallet.mCard.addTokenToBlockchainName();
                                loadedWallet.mSwipeRefreshLayout.setRefreshing(false);
                                loadedWallet.onRefresh();
                                return;
                            }

                            loadedWallet.mCard.setBalanceConfirmed(balance);
                            loadedWallet.mCard.setBalanceUnconfirmed(0L);
                            loadedWallet.mCard.setDecimalBalance(l.toString(10));

                        } catch (JSONException e) {
                            e.printStackTrace();
                            loadedWallet.errorOnUpdate(e.toString());
                        }
                    } else if (request.isMethod(InfuraRequest.METHOD_ETH_GetOutTransactionCount)) {
                        try {
                            String nonce = request.getResultString();
                            nonce = nonce.substring(2);
                            BigInteger count = new BigInteger(nonce, 16);

                            loadedWallet.mCard.SetConfirmTXCount(count);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (request.isMethod(InfuraRequest.METHOD_ETH_SendRawTransaction)) {
                        try {
                            String hashTX = "";

                            try {
                                String tmp = request.getResultString();
                                hashTX = tmp;
                            } catch (JSONException e) {
                                JSONObject msg = request.getAnswer();
                                JSONObject err = msg.getJSONObject("error");
                                hashTX = err.getString("message");
                                LastSignStorage.setLastMessage(loadedWallet.mCard.getWallet(), hashTX);
                                loadedWallet.errorOnUpdate("Failed to send transaction. Try again");
                                return;
                            }

                            if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                hashTX = hashTX.substring(2);
                            }
                            BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                            LastSignStorage.setTxWasSend(loadedWallet.mCard.getWallet());
                            LastSignStorage.setLastMessage(loadedWallet.mCard.getWallet(), "");
                            Log.e("TX_RESULT", hashTX);


                            BigInteger nonce = loadedWallet.mCard.GetConfirmTXCount();
                            nonce.add(BigInteger.valueOf(1));
                            loadedWallet.mCard.SetConfirmTXCount(nonce);
                            Log.e("TX_RESULT", hashTX);

                        } catch (Exception e) {
                            e.printStackTrace();
                            loadedWallet.errorOnUpdate("Failed to send transaction. Try again");
                        }
                    }
                    loadedWallet.updateViews();
                } else {
                    loadedWallet.errorOnUpdate(request.error);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                loadedWallet.errorOnUpdate(e.toString());
            }
        }

        if (loadedWallet.updateTasks.size() == 0)
            loadedWallet.mSwipeRefreshLayout.setRefreshing(false);
    }
}
