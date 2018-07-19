package com.tangem.data.network.task.loaded_wallet;

import android.os.AsyncTask;

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
            CoinEngine engine = CoinEngineFactory.Create(
                    loadedWallet.
                            mCard.getBlockchain());

            for (ElectrumRequest request : requests) {
                try {
                    if (request.error == null) {
                        if (request.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);
                                Long confBalance = request.getResult().getLong("confirmed");
                                Long unconf = request.getResult().getLong("unconfirmed");
                                loadedWallet.mCard.setBalanceRecieved(true);

                                if (sharedCounter != null) {
                                    boolean notEqualBalance = sharedCounter.UpdatePayload(new BigDecimal(String.valueOf(confBalance)));
                                    if (notEqualBalance)
                                        loadedWallet.mCard.setIsBalanceEqual(false);
                                    int counter = sharedCounter.requestCounter.incrementAndGet();
                                    if (counter != 1) {
                                        continue;
                                    }
                                }


                                loadedWallet.mCard.setBalanceConfirmed(confBalance);
                                loadedWallet.mCard.setBalanceUnconfirmed(unconf);
                                loadedWallet.mCard.setDecimalBalance(String.valueOf(confBalance));

                                loadedWallet.mCard.setValidationNodeDescription(getValidationNodeDescription());
                            } catch (JSONException e) {
                                if (sharedCounter != null) {
                                    int errCounter = sharedCounter.errorRequest.incrementAndGet();
                                    loadedWallet.mCard.incFailedBalanceRequestCounter();
                                    if (errCounter >= sharedCounter.allRequest) {
                                        e.printStackTrace();
                                        engine.SwitchNode(loadedWallet.mCard);
                                    }
                                } else {
                                    e.printStackTrace();
                                    engine.SwitchNode(loadedWallet.mCard);
                                }
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_SendTransaction)) {
                            try {
                                String hashTX = request.getResultString();

                                try {
                                    LastSignStorage.setLastMessage(loadedWallet.mCard.getWallet(), hashTX);
                                    if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                        hashTX = hashTX.substring(2);
                                    }
                                    BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                                    LastSignStorage.setTxWasSend(loadedWallet.mCard.getWallet());
                                    LastSignStorage.setLastMessage(loadedWallet.mCard.getWallet(), "");
//                                Log.e("TX_RESULT", hashTX);

                                } catch (Exception e) {
                                    engine.SwitchNode(loadedWallet.mCard);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.SwitchNode(loadedWallet.mCard);
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_ListUnspent)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);

                                JSONArray jsUnspentArray = request.getResultArray();
                                try {
                                    loadedWallet.mCard.getUnspentTransactions().clear();
                                    for (int i = 0; i < jsUnspentArray.length(); i++) {
                                        JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                                        TangemCard.UnspentTransaction trUnspent = new TangemCard.UnspentTransaction();
                                        trUnspent.txID = jsUnspent.getString("tx_hash");
                                        trUnspent.Amount = jsUnspent.getInt("value");
                                        trUnspent.Height = jsUnspent.getInt("height");
                                        loadedWallet.mCard.getUnspentTransactions().add(trUnspent);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    engine.SwitchNode(loadedWallet.mCard);
                                }

                                for (int i = 0; i < jsUnspentArray.length(); i++) {
                                    JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                                    Integer height = jsUnspent.getInt("height");
                                    String hash = jsUnspent.getString("tx_hash");
                                    if (height != -1) {
                                        String nodeAddress = engine.GetNextNode(loadedWallet.mCard);
                                        int nodePort = engine.GetNextNodePort(loadedWallet.mCard);
                                        UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(loadedWallet, nodeAddress, nodePort);

//                                    loadedWallet.updateTasks.add(updateWalletInfoTask);

                                        updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.GetHeader(mWalletAddress, String.valueOf(height)),
                                                ElectrumRequest.GetTransaction(mWalletAddress, hash));
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.SwitchNode(loadedWallet.mCard);
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_GetHistory)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);

                                JSONArray jsHistoryArray = request.getResultArray();
                                try {
                                    loadedWallet.mCard.getHistoryTransactions().clear();
                                    for (int i = 0; i < jsHistoryArray.length(); i++) {
                                        JSONObject jsUnspent = jsHistoryArray.getJSONObject(i);
                                        TangemCard.HistoryTransaction trHistory = new TangemCard.HistoryTransaction();
                                        trHistory.txID = jsUnspent.getString("tx_hash");
                                        trHistory.Height = jsUnspent.getInt("height");
                                        loadedWallet.mCard.getHistoryTransactions().add(trHistory);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    engine.SwitchNode(loadedWallet.mCard);
                                }

                                for (int i = 0; i < jsHistoryArray.length(); i++) {
                                    JSONObject jsUnspent = jsHistoryArray.getJSONObject(i);
                                    Integer height = jsUnspent.getInt("height");
                                    String hash = jsUnspent.getString("tx_hash");
                                    if (height != -1) {

                                        String nodeAddress = engine.GetNode(loadedWallet.mCard);
                                        int nodePort = engine.GetNodePort(loadedWallet.mCard);
                                        UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(loadedWallet, nodeAddress, nodePort);
//                                    loadedWallet.updateTasks.add(updateWalletInfoTask);

                                        updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.GetHeader(mWalletAddress, String.valueOf(height)),
                                                ElectrumRequest.GetTransaction(mWalletAddress, hash));
                                    }

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.SwitchNode(loadedWallet.mCard);
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_GetHeader)) {
                            try {
                                JSONObject jsHeader = request.getResult();
                                try {
                                    loadedWallet.mCard.getHaedersInfo();
                                    loadedWallet.mCard.UpdateHeaderInfo(new TangemCard.HeaderInfo(
                                            jsHeader.getInt("block_height"),
                                            jsHeader.getInt("timestamp")));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    engine.SwitchNode(loadedWallet.mCard);

                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.SwitchNode(loadedWallet.mCard);

                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
                            try {

                                String txHash = request.TxHash;
                                String raw = request.getResultString();

                                List<TangemCard.UnspentTransaction> listTx = loadedWallet.mCard.getUnspentTransactions();
                                for (TangemCard.UnspentTransaction tx : listTx) {
                                    if (tx.txID.equals(txHash)) {
                                        tx.Raw = raw;
                                    }
                                }

                                List<TangemCard.HistoryTransaction> listHTx = loadedWallet.mCard.getHistoryTransactions();
                                for (TangemCard.HistoryTransaction tx : listHTx) {
                                    if (tx.txID.equals(txHash)) {
                                        tx.Raw = raw;
                                        try {
                                            ArrayList<byte[]> prevHashes = BTCUtils.getPrevTX(raw);

                                            boolean isOur = false;
                                            for (byte[] hash : prevHashes) {
                                                String checkID = BTCUtils.toHex(hash);
                                                for (TangemCard.HistoryTransaction txForCheck : listHTx) {
                                                    if (txForCheck.txID == checkID) {
                                                        isOur = true;
                                                    }
                                                }
                                            }

                                            tx.isInput = !isOur;
                                        } catch (BitcoinException e) {
                                            e.printStackTrace();
                                        }
//                                    Log.e("TX", raw);
                                    }
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                engine.SwitchNode(loadedWallet.mCard);
                            }
                        }
                        loadedWallet.updateViews();
                    } else {
                        if (sharedCounter != null) {
                            int errCounter = sharedCounter.errorRequest.incrementAndGet();
                            loadedWallet.mCard.incFailedBalanceRequestCounter();
                            if (errCounter >= sharedCounter.allRequest) {
                                engine.SwitchNode(loadedWallet.mCard);

                            }
                        } else {
                            engine.SwitchNode(loadedWallet.mCard);

                        }

                    }
                } catch (JSONException e) {
                    if (sharedCounter != null) {
                        int errCounter = sharedCounter.errorRequest.incrementAndGet();
                        loadedWallet.mCard.incFailedBalanceRequestCounter();
                        if (errCounter >= sharedCounter.allRequest) {
                            e.printStackTrace();
                        }
                    } else {
                        e.printStackTrace();
                    }
                }
            }

            loadedWallet.srlLoadedWallet.setRefreshing(false);

        } catch (NullPointerException e) {
            e.printStackTrace();

            if (reference.get() != null) {
                reference.get().srlLoadedWallet.setRefreshing(false);
            }
        }


    }
}
