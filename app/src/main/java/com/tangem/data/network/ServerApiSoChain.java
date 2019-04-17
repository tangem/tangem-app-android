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

    private ResponseListener responseListener;

    public interface ResponseListener {
        void onSuccess(SoChain.Response.AddressBalance response);

        void onSuccess(SoChain.Response.TxUnspent response);

        void onFail(String message);
    }

    public void setResponseListener(ResponseListener listener) {
        responseListener = listener;
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
                    responseListener.onSuccess(response.body());
                } else {
                    responseListener.onFail(String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SoChain.Response.AddressBalance> call, @NonNull Throwable t) {
                Log.e(TAG, "requestAddressBalance  onFailure " + t.getMessage());
                responseListener.onFail(String.valueOf(t.getMessage()));
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
                    responseListener.onSuccess(response.body());
                } else {
                    responseListener.onFail(String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SoChain.Response.TxUnspent> call, @NonNull Throwable t) {
                Log.e(TAG, "requestAddressBalance  onFailure " + t.getMessage());
                responseListener.onFail(String.valueOf(t.getMessage()));
            }
        });
    }

}
