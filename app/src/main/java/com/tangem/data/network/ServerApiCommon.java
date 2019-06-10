package com.tangem.data.network;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import android.util.Log;

import com.tangem.App;
import com.tangem.data.network.model.RateInfoResponse;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerApiCommon {
    private static String TAG = ServerApiCommon.class.getSimpleName();

    /**
     * HTTP
     * Estimate fee
     */
    public static final int ESTIMATE_FEE_PRIORITY = 2;
    public static final int ESTIMATE_FEE_NORMAL = 3;
    public static final int ESTIMATE_FEE_MINIMAL = 6;
    private EstimatedFeeListener estimatedFeeListener;

    public interface EstimatedFeeListener {
        void onSuccess(int blockCount, String estimateFeeResponse);
        void onFail(int blockCount, String message);
    }

    public void setBtcEstimatedFeeListener(EstimatedFeeListener listener) {
        estimatedFeeListener = listener;
    }

    public void requestBtcEstimatedFee(int blockCount) {
        EstimatefeeApi estimatefeeApi = App.Companion.getNetworkComponent().getRetrofitEstimatefee().create(EstimatefeeApi.class);

        Call<String> call;
        switch (blockCount) {
            case ESTIMATE_FEE_PRIORITY:
                call = estimatefeeApi.getEstimateFeePriority();
                break;

            case ESTIMATE_FEE_NORMAL:
                call = estimatefeeApi.getEstimateFeeNormal();
                break;

            case ESTIMATE_FEE_MINIMAL:
                call = estimatefeeApi.getEstimateFeeMinimal();
                break;

            default:
                call = estimatefeeApi.getEstimateFeeNormal();
        }

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.code() == 200) {
                    estimatedFeeListener.onSuccess(blockCount, response.body());
                    Log.i(TAG, "requestBtcEstimatedFee         onResponse " + response.code() + "  " + response.body());
                } else
                    estimatedFeeListener.onFail(blockCount, response.body());
                Log.e(TAG, "requestBtcEstimatedFee         onResponse " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                estimatedFeeListener.onFail(blockCount, t.getMessage());
                Log.e(TAG, "requestBtcEstimatedFee onFailure " + t.getMessage());
            }
        });
    }

    /**
     * HTTP
     * Used in Crypto-currency course
     */
    private RateInfoListener rateInfoListener;

    public interface RateInfoListener {
        void onSuccess(RateInfoResponse rateInfoResponse);

        void onFail(String message);
    }

    public void setRateInfoListener(RateInfoListener listener) {
        rateInfoListener = listener;
    }

    public void requestRateInfo(String cryptoId) {
        CoinmarketApi coinmarketApi = App.getNetworkComponent().getRetrofitCoinmarketcap().create(CoinmarketApi.class);

        Call<RateInfoResponse> call = coinmarketApi.getRateInfo(1, cryptoId);

        call.enqueue(new Callback<RateInfoResponse>() {
            @Override
            public void onResponse(@NonNull Call<RateInfoResponse> call, @NonNull Response<RateInfoResponse> response) {
                if (response.code() == 200) {
                    rateInfoDataListener.onSuccess(response.body());
                    Log.i(TAG, "coinmarketcap onResponse " + response.code());
                } else {
                    rateInfoDataListener.onFail("Rate info error:" + String.valueOf(response.code()));
                    Log.e(TAG, "coinmarketcap onResponse " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<RateInfoResponse> call, @NonNull Throwable t) {
                rateInfoDataListener.onFail(String.valueOf(t.getMessage()));
                Log.e(TAG, "coinmarketcap onFailure " + t.getMessage());
            }
        });
//        coinmarketApi.getRateInfoList()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(rateInfoModelList -> {
//                            if (!rateInfoModelList.isEmpty()) {
//                                for (RateInfoResponse rateInfoMode : rateInfoModelList) {
//                                    if (rateInfoMode.getId().equals(cryptoId)) {
//                                        rateInfoDataListener.onSuccess(rateInfoMode);
//                                        Log.i(TAG, "rateInfoData        " + cryptoId + " onResponse " + "200");
//                                    }
//                                }
//                            } else
//                                Log.e(TAG, "rateInfoData        " + cryptoId + " onResponse " + "Empty");
//                        },
//                        // handle error
//                        Throwable::printStackTrace);
    }

    /**
     * HTTP
     * Last version request from GitHub
     */
    private LastVersionListener lastVersionListener;

    public interface LastVersionListener {
        void onSuccess(String lastVersion);
    }

    public void setLastVersionListener(LastVersionListener listener) {
        lastVersionListener = listener;
    }

    public void requestLastVersion() {
        UpdateVersionApi updateVersionApi = App.Companion.getNetworkComponent().getRetrofitGitHubUserContent().create(UpdateVersionApi.class);

        Call<ResponseBody> call = updateVersionApi.getLastVersion();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "requestLastVersion  onResponse " + response.code());
                if (response.code() == 200) {
                    String stringResponse;
                    try {
                        stringResponse = response.body() != null ? response.body().string() : null;
                        lastVersionListener.onSuccess(stringResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "lastVersion onFailure " + t.getMessage());
            }
        });
    }

}