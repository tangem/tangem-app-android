package com.tangem.data.network.task.main;

import android.os.AsyncTask;
import android.util.Log;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.task.ElectrumTask;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.SharedData;
import com.tangem.presentation.fragment.Main;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;

public class RequestWalletInfoTask extends ElectrumTask {
    private WeakReference<Main> reference;

    public RequestWalletInfoTask(Main context, String host, int port) {
        super(host, port);
        reference = new WeakReference<>(context);
    }


    public RequestWalletInfoTask(Main context, String host, int port, SharedData sharedData) {
        super(host, port, sharedData);
        reference = new WeakReference<>(context);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Main main = reference.get();
//        main.requestTasks.remove(this);
    }

    private void FinishWithError(String wallet, String message) {
        Main main = reference.get();
        main.mCardListAdapter.UpdateWalletError(wallet, "Cannot obtain data from blockchain");
    }

    @Override
    protected void onPostExecute(List<ElectrumRequest> requests) {
        super.onPostExecute(requests);
        Main main = reference.get();

//        main.requestTasks.remove(this);
//        Log.i("RequestWalletInfoTask", "onPostExecute[" + String.valueOf(requests.size()) + "]");

        CoinEngine engine = CoinEngineFactory.Create(Blockchain.Bitcoin);

        for (ElectrumRequest request : requests) {
            try {

                if (request.error == null) {
                    if (request.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                        try {
                            Long conf = request.getResult().getLong("confirmed");
                            Long unconf = request.getResult().getLong("unconfirmed");
                            if (sharedCounter != null) {
                                int counter = sharedCounter.requestCounter.incrementAndGet();
                                if (counter != 1) {
                                    continue;
                                }
                            }
                            String mWalletAddress = request.getParams().getString(0);
                            main.mCardListAdapter.UpdateWalletBalance(mWalletAddress, conf, unconf, getValidationNodeDescription());
                        } catch (JSONException e) {
                            if (sharedCounter != null) {
                                int errCounter = sharedCounter.errorRequest.incrementAndGet();
                                if (errCounter == sharedCounter.allRequest) {
                                    e.printStackTrace();
                                    FinishWithError(request.WalletAddress, e.toString());
                                    engine.SwitchNode(null);
                                }
                            } else {
                                e.printStackTrace();
                                FinishWithError(request.WalletAddress, e.toString());
                                engine.SwitchNode(null);
                            }
                        }
                    } else if (request.isMethod(ElectrumRequest.METHOD_ListUnspent)) {
                        try {
                            String mWalletAddress = request.getParams().getString(0);
                            main.mCardListAdapter.UpdateWalletUnspent(mWalletAddress, request.getResultArray());

                            JSONArray unspentList = request.getResultArray();

                            for (int i = 0; i < unspentList.length(); i++) {
                                JSONObject jsUnspent = unspentList.getJSONObject(i);
                                Integer height = jsUnspent.getInt("height");
                                String hash = jsUnspent.getString("tx_hash");
                                if (height != -1) {
                                    RequestWalletInfoTask task = new RequestWalletInfoTask(main, request.Host, request.Port);
//                                    main.requestTasks.add(task);
                                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.GetHeader(mWalletAddress, String.valueOf(height)),
                                            ElectrumRequest.GetTransaction(mWalletAddress, hash));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            FinishWithError(request.WalletAddress, e.toString());
                            engine.SwitchNode(null);
                        }
                    } else if (request.isMethod(ElectrumRequest.METHOD_GetHistory)) {
                        try {
                            String mWalletAddress = request.getParams().getString(0);
                            main.mCardListAdapter.UpdateWalletHistory(mWalletAddress, request.getResultArray());

                            JSONArray historyList = request.getResultArray();

                            for (int i = 0; i < historyList.length(); i++) {
                                JSONObject jsUnspent = historyList.getJSONObject(i);
                                Integer height = jsUnspent.getInt("height");
                                String hash = jsUnspent.getString("tx_hash");
                                if (height != -1) {
                                    RequestWalletInfoTask task = new RequestWalletInfoTask(main, request.Host, request.Port);
//                                    main.requestTasks.add(task);

                                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.GetHeader(mWalletAddress, String.valueOf(height)),
                                            ElectrumRequest.GetTransaction(mWalletAddress, hash));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            FinishWithError(request.WalletAddress, e.toString());
                            engine.SwitchNode(null);
                        }
                    } else if (request.isMethod(ElectrumRequest.METHOD_GetHeader)) {
                        try {
                            String mWalletAddress = request.WalletAddress;

                            main.mCardListAdapter.UpdateWalletHeader(mWalletAddress, request.getResult());

                        } catch (JSONException e) {
                            e.printStackTrace();
                            FinishWithError(request.WalletAddress, e.toString());
                            engine.SwitchNode(null);
                        }
                    } else if (request.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
                        try {
                            Log.e("MainActivityFragment_TX", request.TxHash);
                            String mWalletAddress = request.WalletAddress;
                            String tx = request.TxHash;
                            String raw = request.getResultString();
                            Log.e("MainActivityFragment_R", raw);
                            main.mCardListAdapter.UpdateTransaction(mWalletAddress, tx, raw);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            main.mCardListAdapter.UpdateWalletError(request.WalletAddress, "Cannot obtain data from blockchain");
                            FinishWithError(request.WalletAddress, e.toString());
                            engine.SwitchNode(null);
                        }
                    }
                } else {
                    if (sharedCounter != null) {
                        int errCounter = sharedCounter.errorRequest.incrementAndGet();
                        if (errCounter >= sharedCounter.allRequest) {
                            FinishWithError(request.WalletAddress, request.error);
                            engine.SwitchNode(null);
                        }
                    } else {
                        FinishWithError(request.WalletAddress, request.error);
                        engine.SwitchNode(null);
                    }

                }
            } catch (JSONException e) {
                if (sharedCounter != null) {
                    int errCounter = sharedCounter.errorRequest.incrementAndGet();
                    if (errCounter >= sharedCounter.allRequest) {
                        e.printStackTrace();
                        main.mCardListAdapter.UpdateWalletError(request.WalletAddress, "Cannot obtain data from blockchain");
                        engine.SwitchNode(null);
                    }
                } else {
                    e.printStackTrace();
                    main.mCardListAdapter.UpdateWalletError(request.WalletAddress, "Cannot obtain data from blockchain");
                    engine.SwitchNode(null);
                }
            }
        }
    }

}