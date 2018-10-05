package com.tangem.data.network.task.loaded_wallet;

import android.os.AsyncTask;
import android.util.Log;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.task.ElectrumTask;
import com.tangem.domain.wallet.BitcoinException;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.domain.wallet.SharedData;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.presentation.fragment.LoadedWallet;
import com.tangem.util.BTCUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class UpdateWalletInfoTask extends ElectrumTask {
    private WeakReference<LoadedWallet> reference;

    public UpdateWalletInfoTask(LoadedWallet context, String host, int port) {
        super(host, port);
        reference = new WeakReference<>(context);
    }

    public UpdateWalletInfoTask(LoadedWallet context, String host, int port, SharedData sharedData) {
        super(host, port, sharedData);
        reference = new WeakReference<>(context);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

//    @Override
//    protected void onCancelled() {
//        super.onCancelled();
//        LoadedWallet loadedWallet = reference.get();
//
//        loadedWallet.updateTasks.remove(this);
//        if (loadedWallet.updateTasks.size() == 0) loadedWallet.mSwipeRefreshLayout.setRefreshing(false);
//    }

    @Override
    protected void onPostExecute(List<ElectrumRequest> requests) {
        super.onPostExecute(requests);
        LoadedWallet loadedWallet = reference.get();

//        Log.i("RequestWalletInfoTask", "onPostExecute[" + String.valueOf(loadedWallet.updateTasks.size()) + "]");
//        loadedWallet.updateTasks.remove(this);

        try {
            CoinEngine engine = CoinEngineFactory.create(loadedWallet.getCard().getBlockchain());
            for (ElectrumRequest request : requests) {
                try {
                    if (request.error == null) {
                        // get balance
                        if (request.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                            try {
                                String walletAddress = request.getParams().getString(0);
                                Long confBalance = request.getResult().getLong("confirmed");
                                Long unconfirmedBalance = request.getResult().getLong("unconfirmed");
                                loadedWallet.getCard().setBalanceReceived(true);
                                if (sharedCounter != null) {
                                    boolean notEqualBalance = sharedCounter.updatePayload(new BigDecimal(String.valueOf(confBalance)));
                                    if (notEqualBalance)
                                        loadedWallet.getCard().setIsBalanceEqual(false);
                                    int counter = sharedCounter.requestCounter.incrementAndGet();
                                    if (counter != 1) {
                                        continue;
                                    }
                                }
                                loadedWallet.getCard().setBalanceConfirmed(confBalance);
                                loadedWallet.getCard().setBalanceUnconfirmed(unconfirmedBalance);
                                loadedWallet.getCard().setDecimalBalance(String.valueOf(confBalance));
                                loadedWallet.getCard().setValidationNodeDescription(getValidationNodeDescription());
                            } catch (JSONException e) {
                                if (sharedCounter != null) {
                                    int errCounter = sharedCounter.errorRequest.incrementAndGet();
                                    loadedWallet.getCard().incFailedBalanceRequestCounter();
                                    if (errCounter >= sharedCounter.allRequest) {
                                        e.printStackTrace();
                                        engine.switchNode(loadedWallet.getCard());
                                    }
                                } else {
                                    e.printStackTrace();
                                    engine.switchNode(loadedWallet.getCard());
                                }
                            }
                        }

                        // send transaction
                        else if (request.isMethod(ElectrumRequest.METHOD_SendTransaction)) {
                            try {
                                String hashTX = request.getResultString();
                                try {
                                    LastSignStorage.setLastMessage(loadedWallet.getCard().getWallet(), hashTX);
                                    if (hashTX.startsWith("0x") || hashTX.startsWith("0X"))
                                        hashTX = hashTX.substring(2);
                                    BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                                    LastSignStorage.setTxWasSend(loadedWallet.getCard().getWallet());
                                    LastSignStorage.setLastMessage(loadedWallet.getCard().getWallet(), "");
//                                Log.e("TX_RESULT", hashTX);
                                } catch (Exception e) {
                                    engine.switchNode(loadedWallet.getCard());
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.switchNode(loadedWallet.getCard());
                            }
                        }

                        // list unspent
                        else if (request.isMethod(ElectrumRequest.METHOD_ListUnspent)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);
                                JSONArray jsUnspentArray = request.getResultArray();
                                try {
                                    loadedWallet.getCard().getUnspentTransactions().clear();
                                    for (int i = 0; i < jsUnspentArray.length(); i++) {
                                        JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                                        TangemCard.UnspentTransaction trUnspent = new TangemCard.UnspentTransaction();
                                        trUnspent.txID = jsUnspent.getString("tx_hash");
                                        trUnspent.Amount = jsUnspent.getInt("value");
                                        trUnspent.Height = jsUnspent.getInt("height");
                                        loadedWallet.getCard().getUnspentTransactions().add(trUnspent);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    engine.switchNode(loadedWallet.getCard());
                                }

                                for (int i = 0; i < jsUnspentArray.length(); i++) {
                                    JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                                    Integer height = jsUnspent.getInt("height");
                                    String hash = jsUnspent.getString("tx_hash");
                                    if (height != -1) {
                                        String nodeAddress = engine.getNode(loadedWallet.getCard());
                                        int nodePort = engine.getNodePort(loadedWallet.getCard());
                                        engine.switchNode(loadedWallet.getCard());
                                        UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(loadedWallet, nodeAddress, nodePort);
//                                    loadedWallet.updateTasks.add(updateWalletInfoTask);
                                        updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.getHeader(mWalletAddress, String.valueOf(height)), ElectrumRequest.getTransaction(mWalletAddress, hash));
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.switchNode(loadedWallet.getCard());
                            }
                        }

                        // get history
                        else if (request.isMethod(ElectrumRequest.METHOD_GetHistory)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);
                                JSONArray jsHistoryArray = request.getResultArray();
                                try {
                                    loadedWallet.getCard().getHistoryTransactions().clear();
                                    for (int i = 0; i < jsHistoryArray.length(); i++) {
                                        JSONObject jsUnspent = jsHistoryArray.getJSONObject(i);
                                        TangemCard.HistoryTransaction trHistory = new TangemCard.HistoryTransaction();
                                        trHistory.txID = jsUnspent.getString("tx_hash");
                                        trHistory.Height = jsUnspent.getInt("height");
                                        loadedWallet.getCard().getHistoryTransactions().add(trHistory);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    engine.switchNode(loadedWallet.getCard());
                                }

                                for (int i = 0; i < jsHistoryArray.length(); i++) {
                                    JSONObject jsUnspent = jsHistoryArray.getJSONObject(i);
                                    Integer height = jsUnspent.getInt("height");
                                    String hash = jsUnspent.getString("tx_hash");
                                    if (height != -1) {
                                        String nodeAddress = engine.getNode(loadedWallet.getCard());
                                        int nodePort = engine.getNodePort(loadedWallet.getCard());
                                        engine.switchNode(loadedWallet.getCard());
                                        UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(loadedWallet, nodeAddress, nodePort);
//                                    loadedWallet.updateTasks.add(updateWalletInfoTask);
                                        updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.getHeader(mWalletAddress, String.valueOf(height)), ElectrumRequest.getTransaction(mWalletAddress, hash));
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.switchNode(loadedWallet.getCard());
                            }
                        }

                        // get header
                        else if (request.isMethod(ElectrumRequest.METHOD_GetHeader)) {
                            try {
                                JSONObject jsHeader = request.getResult();
                                try {
                                    loadedWallet.getCard().getHaedersInfo();
                                    loadedWallet.getCard().UpdateHeaderInfo(new TangemCard.HeaderInfo(jsHeader.getInt("block_height"), jsHeader.getInt("timestamp")));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    engine.switchNode(loadedWallet.getCard());
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.switchNode(loadedWallet.getCard());
                            }
                        }

                        // get transaction
                        else if (request.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
                            try {
                                String txHash = request.txHash;
                                String raw = request.getResultString();
                                List<TangemCard.UnspentTransaction> listTx = loadedWallet.getCard().getUnspentTransactions();
                                for (TangemCard.UnspentTransaction tx : listTx) {
                                    if (tx.txID.equals(txHash))
                                        tx.Raw = raw;
                                }
                                List<TangemCard.HistoryTransaction> listHTx = loadedWallet.getCard().getHistoryTransactions();
                                for (TangemCard.HistoryTransaction tx : listHTx) {
                                    if (tx.txID.equals(txHash)) {
                                        tx.Raw = raw;
                                        try {
                                            ArrayList<byte[]> prevHashes = BTCUtils.getPrevTX(raw);
                                            boolean isOur = false;
                                            for (byte[] hash : prevHashes) {
                                                String checkID = BTCUtils.toHex(hash);
                                                for (TangemCard.HistoryTransaction txForCheck : listHTx) {
                                                    if (txForCheck.txID.equals(checkID))
                                                        isOur = true;
                                                }
                                            }
                                            tx.isInput = !isOur;
                                        } catch (BitcoinException e) {
                                            e.printStackTrace();
                                        }
                                    Log.e("TX", raw);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.switchNode(loadedWallet.getCard());
                            }
                        }
                        loadedWallet.updateViews();

                    } else {
                        if (sharedCounter != null) {
                            int errCounter = sharedCounter.errorRequest.incrementAndGet();
                            loadedWallet.getCard().incFailedBalanceRequestCounter();
                            if (errCounter >= sharedCounter.allRequest)
                                engine.switchNode(loadedWallet.getCard());
                        } else
                            engine.switchNode(loadedWallet.getCard());
                    }
                } catch (JSONException e) {
                    if (sharedCounter != null) {
                        int errCounter = sharedCounter.errorRequest.incrementAndGet();
                        loadedWallet.getCard().incFailedBalanceRequestCounter();
                        if (errCounter >= sharedCounter.allRequest)
                            e.printStackTrace();
                    } else {
                        e.printStackTrace();
                    }
                }
            }

            loadedWallet.getSrlLoadedWallet().setRefreshing(false);

        } catch (NullPointerException e) {
            e.printStackTrace();
            if (reference.get() != null)
                reference.get().getSrlLoadedWallet().setRefreshing(false);
        }
    }

}
