package com.tangem.data.network;

import android.util.Log;

import com.tangem.App;
import com.tangem.data.Blockchain;
import com.tangem.data.network.model.SoChain;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerApiSoChain {

    public static String NETWORK_BTC = "BTC";

    private static String TAG = ServerApiSoChain.class.getSimpleName();

    private int requestsCount = 0;

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }


    public interface AddressInfoListener {
        void onSuccess(SoChain.Response.AddressBalance response);

        void onSuccess(SoChain.Response.TxUnspent response);

        void onFail(String message);
    }

    private AddressInfoListener addressInfoListener;

    public void setAddressInfoListener(AddressInfoListener listener) {
        addressInfoListener = listener;
    }

    public interface SendTxListener {
        void onSuccess(SoChain.Response.SendTx response);

        void onFail(String message);
    }

    private SendTxListener sendTxListener;

    public void setSendTxListener(SendTxListener listener) {
        sendTxListener = listener;
    }


    public interface TransactionInfoListener {
        void onSuccess(SoChain.Response.GetTx response);

        void onFail(String message);
    }

    private TransactionInfoListener txInfoListener;

    public void setTransactionInfoListener(TransactionInfoListener listener) {
        txInfoListener=listener;
    }


    private String getNetwork(Blockchain blockchain) throws Exception {
        switch (blockchain) {
            case Bitcoin:
                return "BTC";
            case BitcoinTestNet:
                return "BTCTEST";
            case Litecoin:
                return "LTC";
            default:
                throw new Exception("SoChainAPI don't support blockchain " + blockchain.getID());
        }
    }

    public void requestAddressBalance(Blockchain blockchain, String wallet) throws Exception {
        requestsCount++;
        SoChainApi api = App.Companion.getNetworkComponent().getRetrofitSoChain().create(SoChainApi.class);

        Call<SoChain.Response.AddressBalance> call = api.getAddressBalance(getNetwork(blockchain), wallet);
        call.enqueue(new Callback<SoChain.Response.AddressBalance>() {
            @Override
            public void onResponse(@NonNull Call<SoChain.Response.AddressBalance> call, @NonNull Response<SoChain.Response.AddressBalance> response) {
                Log.i(TAG, "requestAddressBalance onResponse " + response.code());
                if (response.code() == 200) {
                    requestsCount--;
                    addressInfoListener.onSuccess(response.body());
                } else {
                    addressInfoListener.onFail(String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SoChain.Response.AddressBalance> call, @NonNull Throwable t) {
                Log.e(TAG, "requestAddressBalance  onFailure " + t.getMessage());
                addressInfoListener.onFail(String.valueOf(t.getMessage()));
            }
        });
    }

    public void requestUnspentTx(Blockchain blockchain, String wallet) throws Exception {
        requestsCount++;
        SoChainApi api = App.Companion.getNetworkComponent().getRetrofitSoChain().create(SoChainApi.class);

        Call<SoChain.Response.TxUnspent> call = api.getUnspentTx(getNetwork(blockchain), wallet);
        call.enqueue(new Callback<SoChain.Response.TxUnspent>() {
            @Override
            public void onResponse(@NonNull Call<SoChain.Response.TxUnspent> call, @NonNull Response<SoChain.Response.TxUnspent> response) {
                Log.i(TAG, "requestAddressBalance onResponse " + response.code());
                if (response.code() == 200) {
                    requestsCount--;
                    addressInfoListener.onSuccess(response.body());
                } else {
                    addressInfoListener.onFail(String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SoChain.Response.TxUnspent> call, @NonNull Throwable t) {
                Log.e(TAG, "requestAddressBalance  onFailure " + t.getMessage());
                addressInfoListener.onFail(String.valueOf(t.getMessage()));
            }
        });
    }

    public void requestSendTransaction(Blockchain blockchain, String txHEX) throws Exception {
        requestsCount++;
        SoChainApi api = App.Companion.getNetworkComponent().getRetrofitSoChain().create(SoChainApi.class);

        SoChain.Request.SendTx tx=new SoChain.Request.SendTx();
        tx.setTx_hex(txHEX);
        Call<SoChain.Response.SendTx> call = api.sendTransaction(getNetwork(blockchain), tx);
        call.enqueue(new Callback<SoChain.Response.SendTx>() {
            @Override
            public void onResponse(@NonNull Call<SoChain.Response.SendTx> call, @NonNull Response<SoChain.Response.SendTx> response) {
                Log.i(TAG, "requestAddressBalance onResponse " + response.code());
                if (response.code() == 200) {
                    requestsCount--;
                    sendTxListener.onSuccess(response.body());
                } else {
                    sendTxListener.onFail(String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SoChain.Response.SendTx> call, @NonNull Throwable t) {
                Log.e(TAG, "requestAddressBalance  onFailure " + t.getMessage());
                sendTxListener.onFail(String.valueOf(t.getMessage()));
            }
        });
    }

    public void requestTransactionInfo(Blockchain blockchain, String txId) throws Exception {
        requestsCount++;
        SoChainApi api = App.Companion.getNetworkComponent().getRetrofitSoChain().create(SoChainApi.class);

        Call<SoChain.Response.GetTx> call = api.getTx(getNetwork(blockchain), txId);
        call.enqueue(new Callback<SoChain.Response.GetTx>() {
            @Override
            public void onResponse(@NonNull Call<SoChain.Response.GetTx> call, @NonNull Response<SoChain.Response.GetTx> response) {
                Log.i(TAG, "requestAddressBalance onResponse " + response.code());
                if (response.code() == 200) {
                    requestsCount--;
                    txInfoListener.onSuccess(response.body());
                } else {
                    txInfoListener.onFail(String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SoChain.Response.GetTx> call, @NonNull Throwable t) {
                Log.e(TAG, "requestAddressBalance  onFailure " + t.getMessage());
                txInfoListener.onFail(String.valueOf(t.getMessage()));
            }
        });
    }

}
