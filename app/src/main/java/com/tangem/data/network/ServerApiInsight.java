package com.tangem.data.network;

import android.util.Log;

import com.tangem.data.network.model.InsightBody;
import com.tangem.data.network.model.InsightResponse;

import java.util.List;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServerApiInsight {
    private static String TAG = ServerApiInsight.class.getSimpleName();

    public static final String INSIGHT_ADDRESS = "/addr/{address}";
    public static final String INSIGHT_UNSPENT_OUTPUTS = "/addr/{address}/utxo";
    public static final String INSIGHT_TRANSACTION = "/rawtx/{txId}";
    public static final String INSIGHT_FEE = "/utils/estimatefee?nbBlocks=2,3,6";
    public static final String INSIGHT_SEND = "/tx/send";

    private int requestsCount = 0;

    public static String lastNode;

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    private ResponseListener responseListener;

    public interface ResponseListener {
        void onSuccess(String method, InsightResponse insightResponse);

        void onSuccess(String method, List<InsightResponse> utxoList);

        void onFail(String method, String message);
    }

    public void setResponseListener(ResponseListener listener) {
        responseListener = listener;
    }

    public void requestData(String method, String wallet, String tx) {
        requestsCount++;
        String insightURL = "http://130.185.109.17:3001/insigth-api"; //TODO: make random selection
        this.lastNode = insightURL; //TODO: show node instead of URL

        Retrofit retrofitInsight = new Retrofit.Builder()
                .baseUrl(insightURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

//        InsightApi insightApi = App.getNetworkComponent().getRetrofitInsight(insightURL).create(InsightApi.class);
        InsightApi insightApi = retrofitInsight.create(InsightApi.class);

        if (method.equals(INSIGHT_UNSPENT_OUTPUTS)) {
            Call<List<InsightResponse>> call = insightApi.insightUnspent(wallet);
            call.enqueue(new Callback<List<InsightResponse>>() {
                @Override
                public void onResponse(@NonNull Call<List<InsightResponse>> call, @NonNull Response<List<InsightResponse>> response) {
                    requestsCount--;

                    if (response.code() == 200) {
                        responseListener.onSuccess(method, response.body());
                        Log.i(TAG, "requestData " + method + " onResponse " + response.code());
                    } else {
                        responseListener.onFail(method, String.valueOf(response.code()));
                        Log.e(TAG, "requestData " + method + " onResponse " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<InsightResponse>> call, @NonNull Throwable t) {
                    requestsCount--;
                    responseListener.onFail(method, String.valueOf(t.getMessage()));
                    Log.e(TAG, "requestData " + method + " onFailure " + t.getMessage());
                }
            });

        } else {
            Call<InsightResponse> call;

            switch (method) {
                case INSIGHT_ADDRESS:
                    call = insightApi.insightAddress(wallet);
                    break;

                case INSIGHT_TRANSACTION:
                    call = insightApi.insightTransaction(tx);
                    break;

                case INSIGHT_FEE:
                    call = insightApi.insightFee();
                    break;

                case INSIGHT_SEND:
                    call = insightApi.insightSend(new InsightBody(tx));
                    break;

                default:
                    call = insightApi.insightAddress(wallet);
                    break;
            }
            call.enqueue(new Callback<InsightResponse>() {
                @Override
                public void onResponse(@NonNull Call<InsightResponse> call, @NonNull Response<InsightResponse> response) {
                    requestsCount--;

                    if (response.code() == 200) {
                        responseListener.onSuccess(method, response.body());
                        Log.i(TAG, "requestData " + method + " onResponse " + response.code());
                    } else {
                        responseListener.onFail(method, String.valueOf(response.code()));
                        Log.e(TAG, "requestData " + method + " onResponse " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<InsightResponse> call, @NonNull Throwable t) {
                    requestsCount--;
                    responseListener.onFail(method, String.valueOf(t.getMessage()));
                    Log.e(TAG, "requestData " + method + " onFailure " + t.getMessage());
                }
            });
        }
    }
}
