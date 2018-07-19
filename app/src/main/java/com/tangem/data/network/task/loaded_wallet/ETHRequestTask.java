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

                            loadedWallet.getMCard().setBalanceConfirmed(balance);
                            loadedWallet.getMCard().setBalanceUnconfirmed(0L);
                            if (loadedWallet.getMCard().getBlockchain() != Blockchain.Token)
                                loadedWallet.getMCard().setDecimalBalance(l.toString(10));
                            loadedWallet.getMCard().setDecimalBalanceAlter(l.toString(10));

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
                                loadedWallet.getMCard().setBlockchainID(Blockchain.Ethereum.getID());
                                loadedWallet.getMCard().addTokenToBlockchainName();
                                loadedWallet.getSrlLoadedWallet().setRefreshing(false);
                                loadedWallet.refresh();
                                return;
                            }

                            loadedWallet.getMCard().setBalanceConfirmed(balance);
                            loadedWallet.getMCard().setBalanceUnconfirmed(0L);
                            loadedWallet.getMCard().setDecimalBalance(l.toString(10));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (request.isMethod(InfuraRequest.METHOD_ETH_GetOutTransactionCount)) {
                        try {
                            String nonce = request.getResultString();
                            nonce = nonce.substring(2);
                            BigInteger count = new BigInteger(nonce, 16);

                            loadedWallet.getMCard().SetConfirmTXCount(count);
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
                                LastSignStorage.setLastMessage(loadedWallet.getMCard().getWallet(), hashTX);
                                return;
                            }

                            if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                hashTX = hashTX.substring(2);
                            }
                            BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                            LastSignStorage.setTxWasSend(loadedWallet.getMCard().getWallet());
                            LastSignStorage.setLastMessage(loadedWallet.getMCard().getWallet(), "");
                            Log.e("TX_RESULT", hashTX);


                            BigInteger nonce = loadedWallet.getMCard().GetConfirmTXCount();
                            nonce.add(BigInteger.valueOf(1));
                            loadedWallet.getMCard().SetConfirmTXCount(nonce);
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
