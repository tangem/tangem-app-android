package com.tangem.data.network;

import android.util.Log;

import com.tangem.App;
import com.tangem.data.network.model.RippleBody;
import com.tangem.data.network.model.RippleResponse;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
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

    private int requestsCount=0;

    public static String lastNode;

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    private ResponseListener responseListener;

    public interface ResponseListener {
        void onSuccess(String method, RippleResponse infuraResponse);

        void onFail(String method, String message);
    }

    public void setResponseListener(ResponseListener listener) {
        responseListener = listener;
    }

    public void requestData(String method, int id, String wallet, String tx) {
        requestsCount++;
        String rippleURL = "http://s1.ripple.com:51234/"; //TODO: make random selection
        lastNode = rippleURL; //TODO: show node instead of URL

        Retrofit retrofitRipple = new Retrofit.Builder()
                .baseUrl(rippleURL)
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
