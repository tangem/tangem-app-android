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

                            loadedWallet.getCard().setBalanceConfirmed(balance);
                            loadedWallet.getCard().setBalanceUnconfirmed(0L);
                            if (loadedWallet.getCard().getBlockchain() != Blockchain.Token)
                                loadedWallet.getCard().setDecimalBalance(l.toString(10));
                            loadedWallet.getCard().setDecimalBalanceAlter(l.toString(10));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (request.isMethod(InfuraRequest.METHOD_ETH_Call)) {
                        try {
                            String balanceCap = request.getResultString();
                            balanceCap = balanceCap.substring(2);
                            BigInteger l = new BigInteger(balanceCap, 16);
                            Long balance = l.longValue();

                            if (l.compareTo(BigInteger.ZERO) == 0) {
                                loadedWallet.getCard().setBlockchainID(Blockchain.Ethereum.getID());
                                loadedWallet.getCard().addTokenToBlockchainName();
                                loadedWallet.getSrlLoadedWallet().setRefreshing(false);
                                loadedWallet.refresh();
                                return;
                            }

                            loadedWallet.getCard().setBalanceConfirmed(balance);
                            loadedWallet.getCard().setBalanceUnconfirmed(0L);
                            loadedWallet.getCard().setDecimalBalance(l.toString(10));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (request.isMethod(InfuraRequest.METHOD_ETH_GetOutTransactionCount)) {
                        try {
                            String nonce = request.getResultString();
                            nonce = nonce.substring(2);
                            BigInteger count = new BigInteger(nonce, 16);

                            loadedWallet.getCard().setConfirmTXCount(count);
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
                                LastSignStorage.setLastMessage(loadedWallet.getCard().getWallet(), hashTX);
                                return;
                            }

                            if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                hashTX = hashTX.substring(2);
                            }
                            BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                            LastSignStorage.setTxWasSend(loadedWallet.getCard().getWallet());
                            LastSignStorage.setLastMessage(loadedWallet.getCard().getWallet(), "");
                            Log.e("TX_RESULT", hashTX);


                            BigInteger nonce = loadedWallet.getCard().GetConfirmTXCount();
                            nonce.add(BigInteger.valueOf(1));
                            loadedWallet.getCard().setConfirmTXCount(nonce);
                            Log.e("TX_RESULT", hashTX);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    loadedWallet.updateViews();
                } else {

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        loadedWallet.getSrlLoadedWallet().setRefreshing(false);
    }
}
