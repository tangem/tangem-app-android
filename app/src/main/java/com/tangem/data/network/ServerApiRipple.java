package com.tangem.data.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.tangem.data.network.model.RippleBody;
import com.tangem.data.network.model.RippleResponse;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServerApiRipple {
    private static String TAG = ServerApiRipple.class.getSimpleName();

    public static final String RIPPLE_ACCOUNT_INFO = "account_info";
    public static final String RIPPLE_ACCOUNT_UNCONFIRMED = "account_unconfirmed";
    public static final String RIPPLE_SUBMIT = "submit";
    public static final String RIPPLE_FEE = "fee";
    public static final String RIPPLE_SERVER_STATE = "server_state";

    private int requestsCount = 0;

    private final String rippleURL1 = "https://s1.ripple.com:51234"; //TODO: make random selection, add more?, move
    private final String rippleURL2 = "https://s2.ripple.com:51234";

    private String currentURL = rippleURL1;

    public String getCurrentURL() {
        return currentURL;
    }

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    private ResponseListener responseListener;

    public interface ResponseListener {
        void onSuccess(String method, RippleResponse rippleResponse);

        void onFail(String method, String message);
    }

    public void setResponseListener(ResponseListener listener) {
        responseListener = listener;
    }

    public void requestData(String method, String wallet, String tx) {
        requestsCount++;

        Retrofit retrofitRipple = new Retrofit.Builder()
                .baseUrl(currentURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RippleApi rippleApi = retrofitRipple.create(RippleApi.class);

        RippleBody rippleBody;
        HashMap<String, String> paramsMap;

        switch (method) {
            case RIPPLE_ACCOUNT_INFO:
                paramsMap = new HashMap<>();
                paramsMap.put("account", wallet);
                paramsMap.put("ledger_index", "validated");
                rippleBody = new RippleBody(method, paramsMap);
                break;

            case RIPPLE_ACCOUNT_UNCONFIRMED:
                paramsMap = new HashMap<>();
                paramsMap.put("account", wallet);
                paramsMap.put("ledger_index", "current");
//                paramsMap.put("queue", "true"); TODO: make queue check if needed
                rippleBody = new RippleBody(RIPPLE_ACCOUNT_INFO, paramsMap);
                break;

            case RIPPLE_SERVER_STATE:
                rippleBody = new RippleBody(method, new HashMap<>());
                break;

            case RIPPLE_FEE:
                rippleBody = new RippleBody(method, new HashMap<>());
                break;

            case RIPPLE_SUBMIT:
                paramsMap = new HashMap<>();
                paramsMap.put("tx_blob", tx);
                rippleBody = new RippleBody(method, paramsMap);
                break;

            default:
                rippleBody = new RippleBody();
        }

        Call<RippleResponse> call = rippleApi.ripple(rippleBody);
        call.enqueue(new Callback<RippleResponse>() {
            @Override
            public void onResponse(@NonNull Call<RippleResponse> call, @NonNull Response<RippleResponse> response) {
                if (response.code() == 200) {
                    requestsCount--;
                    responseListener.onSuccess(method, response.body());
                    Log.i(TAG, "requestData " + method + " onResponse " + response.code());
                } else {
                    retryRequest(method, rippleBody);
                    Log.e(TAG, "requestData " + method + " onResponse " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<RippleResponse> call, @NonNull Throwable t) {
                retryRequest(method, rippleBody);
                Log.e(TAG, "requestData " + method + " onFailure " + t.getMessage());
            }
        });
    }

    private void retryRequest(String method, RippleBody rippleBody) {
        currentURL = rippleURL2;

        Retrofit retrofitRipple = new Retrofit.Builder()
                .baseUrl(currentURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RippleApi rippleApi = retrofitRipple.create(RippleApi.class);

        Call<RippleResponse> call = rippleApi.ripple(rippleBody);
        call.enqueue(new Callback<RippleResponse>() {
            @Override
            public void onResponse(@NonNull Call<RippleResponse> call, @NonNull Response<RippleResponse> response) {
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
            public void onFailure(@NonNull Call<RippleResponse> call, @NonNull Throwable t) {
                responseListener.onFail(method, String.valueOf(t.getMessage()));
                Log.e(TAG, "requestData " + method + " onFailure " + t.getMessage());
            }
        });
    }
}
