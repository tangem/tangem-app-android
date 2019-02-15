package com.tangem.data.network;

import android.util.Log;

import com.tangem.data.network.model.AdaliteResponse;
import com.tangem.data.network.model.AdaliteResponseUtxo;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ServerApiAdalite {
    private static String TAG = ServerApiAdalite.class.getSimpleName();

    public static final String ADALITE_ADDRESS = "/api/addresses/summary/{address}";
    public static final String ADALITE_UNSPENT_OUTPUTS = "/api/bulk/addresses/utxo";
    //    public static final String ADALITE_TRANSACTION = "/api/txs/raw/{txId}}";
    public static final String ADALITE_SEND = "/tx/send";

    private int requestsCount = 0;

    public static String lastNode;

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    private ResponseListener responseListener;

    public interface ResponseListener {
        void onSuccess(String method, AdaliteResponse adaliteResponse);

        void onSuccess(String method, AdaliteResponseUtxo adaliteResponseUtxo);

        void onFail(String method, String message);
    }

    public void setResponseListener(ResponseListener listener) {
        responseListener = listener;
    }

    public void requestData(String method, String wallet, String tx) {
        requestsCount++;
        String adaliteURL = "https://explorer.adalite.io"; //TODO: make random selection
        this.lastNode = adaliteURL; //TODO: show node instead of URL

        Retrofit retrofitAdalite = new Retrofit.Builder()
                .baseUrl(adaliteURL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AdaliteApi adaliteApi = retrofitAdalite.create(AdaliteApi.class);

        if (method.equals(ADALITE_UNSPENT_OUTPUTS)) {
            Call<AdaliteResponseUtxo> call = adaliteApi.adaliteUnspent("[\"" + wallet + "\"]");
            call.enqueue(new Callback<AdaliteResponseUtxo>() {
                @Override
                public void onResponse(@NonNull Call<AdaliteResponseUtxo> call, @NonNull Response<AdaliteResponseUtxo> response) {
                    if (response.code() == 200) {
                        requestsCount--;
                        responseListener.onSuccess(method, response.body());
                        Log.i(TAG, "requestData " + method + " onResponse " + response.code());
                    } else {
                        responseListener.onFail(method, String.valueOf(response.code()));
                        Log.e(TAG, "requestData " + method + " onResponse " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AdaliteResponseUtxo> call, @NonNull Throwable t) {
                    responseListener.onFail(method, String.valueOf(t.getMessage()));
                    Log.e(TAG, "requestData " + method + " onFailure " + t.getMessage());
                }
            });
        } else {
            Call<AdaliteResponse> call;
            switch (method) {
                case ADALITE_ADDRESS:
                    call = adaliteApi.adaliteAddress(wallet);
                    break;

//            case ADALITE_TRANSACTION:
//                call = adaliteApi.adaliteTransaction(tx);
//                break;

                case ADALITE_SEND:
                    call = adaliteApi.adaliteSend(tx);
                    break;

                default:
                    call = adaliteApi.adaliteAddress(wallet);
                    break;
            }

            call.enqueue(new Callback<AdaliteResponse>() {
                @Override
                public void onResponse(@NonNull Call<AdaliteResponse> call, @NonNull Response<AdaliteResponse> response) {
                    if (response.code() == 200) {
                        requestsCount--;
                        responseListener.onSuccess(method, response.body());
                        Log.i(TAG, "requestData " + method + " onResponse " + response.code());
                    } else {
                        responseListener.onFail(method, String.valueOf(response.code()));
                        Log.e(TAG, "requestData " + method + " onResponse " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AdaliteResponse> call, @NonNull Throwable t) {
                    responseListener.onFail(method, String.valueOf(t.getMessage()));
                    Log.e(TAG, "requestData " + method + " onFailure " + t.getMessage());
                }
            });
        }
    }
}

