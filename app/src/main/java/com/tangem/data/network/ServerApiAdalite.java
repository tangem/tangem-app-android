package com.tangem.data.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.tangem.data.network.model.AdaliteBody;
import com.tangem.data.network.model.AdaliteResponse;
import com.tangem.data.network.model.AdaliteResponseUtxo;

import java.util.List;

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
    public static final String ADALITE_SEND = "/api/v2/txs/signed";

    private int requestsCount = 0;

    private final String adaliteURL1 = "https://explorer3.adalite.io"; //TODO: make random selection, add more?, move
    private final String adaliteURL2 = "https://nodes.southeastasia.cloudapp.azure.com";

    private String currentURL = adaliteURL1;

    public String getCurrentURL() {
        return currentURL;
    }

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    private ResponseListener responseListener;

    public interface ResponseListener {
        void onSuccess(String method, AdaliteResponse adaliteResponse);

        void onSuccess(String method, AdaliteResponseUtxo adaliteResponseUtxo);

        void onSuccess(String method, String stringResponse);

        void onFail(String method, String message);
    }

    public void setResponseListener(ResponseListener listener) {
        responseListener = listener;
    }

    public void requestData(String method, String wallet, String tx) {
        requestData(method, wallet, tx, false);
    }

    public void requestData(String method, String wallet, String tx, boolean isRetry) {
        requestsCount++;

        Retrofit retrofitAdalite = new Retrofit.Builder()
                .baseUrl(currentURL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AdaliteApi adaliteApi = retrofitAdalite.create(AdaliteApi.class);

        switch (method) {
            case ADALITE_ADDRESS:

                Call<AdaliteResponse> addressCall = adaliteApi.adaliteAddress(wallet);
                addressCall.enqueue(new Callback<AdaliteResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AdaliteResponse> call, @NonNull Response<AdaliteResponse> response) {
                        requestsCount--;

                        if (response.code() == 200) {
                            responseListener.onSuccess(method, response.body());
                            Log.i(TAG, "requestData " + method + " onResponse " + response.code());
                        } else {
                            Log.e(TAG, "requestData " + method + " onResponse " + response.code());

                            if (!isRetry) {
                                retryRequest(method, wallet, tx);
                            } else {
                                responseListener.onFail(method, String.valueOf(response.code()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AdaliteResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "requestData " + method + " onFailure " + t.getMessage());
                        requestsCount--;

                        if (!isRetry) {
                            retryRequest(method, wallet, tx);
                        } else {
                            responseListener.onFail(method, String.valueOf(t.getMessage()));
                        }
                    }
                });
                break;

            case ADALITE_UNSPENT_OUTPUTS:
            Call<AdaliteResponseUtxo> outputsCall = adaliteApi.adaliteUnspent("[\"" + wallet + "\"]");
            outputsCall.enqueue(new Callback<AdaliteResponseUtxo>() {
                @Override
                public void onResponse(@NonNull Call<AdaliteResponseUtxo> call, @NonNull Response<AdaliteResponseUtxo> response) {
                    requestsCount--;

                    if (response.code() == 200) {
                        responseListener.onSuccess(method, response.body());
                        Log.i(TAG, "requestData " + method + " onResponse " + response.code());
                    } else {
                        Log.e(TAG, "requestData " + method + " onResponse " + response.code());

                        if (!isRetry) {
                            retryRequest(method, wallet, tx);
                        } else {
                            responseListener.onFail(method, String.valueOf(response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AdaliteResponseUtxo> call, @NonNull Throwable t) {
                    Log.e(TAG, "requestData " + method + " onFailure " + t.getMessage());
                    requestsCount--;

                    if (!isRetry) {
                        retryRequest(method, wallet, tx);
                    } else {
                        responseListener.onFail(method, String.valueOf(t.getMessage()));
                    }
                }
            });
            break;

            case ADALITE_SEND:
                Call<String> sendCall = adaliteApi.adaliteSend(new AdaliteBody(tx));
                sendCall.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        requestsCount--;

                        if (response.code() == 200) {
                            responseListener.onSuccess(method, response.body());
                            Log.i(TAG, "requestData " + method + " onResponse " + response.code());
                        } else {
                            Log.e(TAG, "requestData " + method + " onResponse " + response.code());

                            if (!isRetry) {
                                retryRequest(method, wallet, tx);
                            } else {
                                responseListener.onFail(method, String.valueOf(response.code()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        Log.e(TAG, "requestData " + method + " onFailure " + t.getMessage());
                        requestsCount--;

                        if (!isRetry) {
                            retryRequest(method, wallet, tx);
                        } else {
                            responseListener.onFail(method, String.valueOf(t.getMessage()));
                        }
                    }
                });
                break;

                default:
                    requestsCount--;
                    responseListener.onFail(method, "undeclared method");
                    Log.e(TAG, "requestData " + method + " onFailure - undeclared method");
                    break;
            }

    }

    private void retryRequest(String method, String wallet, String tx) {
//        currentURL = adaliteURL2;
        requestData(method, wallet, tx, true);
    }
}

