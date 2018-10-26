package com.tangem.data.network;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.util.Log;

import com.tangem.App;
import com.tangem.data.network.model.CardVerifyAndGetInfo;
import com.tangem.data.network.model.InfuraBody;
import com.tangem.data.network.model.InfuraResponse;
import com.tangem.data.network.model.RateInfoResponse;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.util.Util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerApiHelper {
    private static String TAG = ServerApiHelper.class.getSimpleName();

    /**
     * HTTP
     * Estimate fee
     */
    public static final int ESTIMATE_FEE_PRIORITY = 2;
    public static final int ESTIMATE_FEE_NORMAL = 3;
    public static final int ESTIMATE_FEE_MINIMAL = 6;
    private EstimateFeeListener estimateFeeListener;

    public interface EstimateFeeListener {
        void onSuccess(int blockCount, String estimateFeeResponse);
        void onFail(String message);
    }

    public void setEstimateFee(EstimateFeeListener listener) {
        estimateFeeListener = listener;
    }

    public void estimateFee(int blockCount) {
        EstimatefeeApi estimatefeeApi = App.getNetworkComponent().getRetrofitEstimatefee().create(EstimatefeeApi.class);

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
                    estimateFeeListener.onSuccess(blockCount, response.body());
                    Log.i(TAG, "estimateFee         onResponse " + response.code() + "  " + response.body());
                } else
                    estimateFeeListener.onFail(response.body());
                Log.e(TAG, "estimateFee         onResponse " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                estimateFeeListener.onFail(t.getMessage());
                Log.e(TAG, "estimateFee onFailure " + t.getMessage());
            }
        });
    }

    /**
     * HTTP
     * Infura
     * <p>
     * eth_getBalance
     * eth_getTransactionCount
     * eth_call
     * eth_sendRawTransaction
     * eth_gasPrice
     */
    public static final String INFURA_ETH_GET_BALANCE = "eth_getBalance";
    public static final String INFURA_ETH_GET_TRANSACTION_COUNT = "eth_getTransactionCount";
    public static final String INFURA_ETH_GET_PENDING_COUNT = "eth_getPendingCount";
    public static final String INFURA_ETH_CALL = "eth_call";
    public static final String INFURA_ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction";
    public static final String INFURA_ETH_GAS_PRICE = "eth_gasPrice";

    private InfuraBodyListener infuraBodyListener;

    public interface InfuraBodyListener {
        void onSuccess(String method, InfuraResponse infuraResponse);
        void onFail(String method, String message);
    }

    public void setInfuraResponse(InfuraBodyListener listener) {
        infuraBodyListener = listener;
    }

    public void infura(String method, int id, String wallet, String contract, String tx) {
        InfuraApi infuraApi = App.getNetworkComponent().getRetrofitInfura().create(InfuraApi.class);

        InfuraBody infuraBody;
        switch (method) {
            case INFURA_ETH_GET_BALANCE:
            case INFURA_ETH_GET_TRANSACTION_COUNT:
                infuraBody = new InfuraBody(method, new String[]{wallet, "latest"}, id);
                break;
            case INFURA_ETH_GET_PENDING_COUNT:
                infuraBody = new InfuraBody(INFURA_ETH_GET_TRANSACTION_COUNT, new String[]{wallet, "pending"}, id);
                break;
            case INFURA_ETH_CALL:
                String address = wallet.substring(2);
                infuraBody = new InfuraBody(method, new Object[]{new InfuraBody.EthCallParams("0x70a08231000000000000000000000000" + address, contract), "latest"}, id);
                break;

            case INFURA_ETH_SEND_RAW_TRANSACTION:
                infuraBody = new InfuraBody(method, new String[]{tx}, id);
                break;

            case INFURA_ETH_GAS_PRICE:
                infuraBody = new InfuraBody(method, id);
                break;

            default:
                infuraBody = new InfuraBody();
        }

        Call<InfuraResponse> call = infuraApi.infura(infuraBody);
        call.enqueue(new Callback<InfuraResponse>() {
            @Override
            public void onResponse(@NonNull Call<InfuraResponse> call, @NonNull Response<InfuraResponse> response) {
                if (response.code() == 200) {
                    infuraBodyListener.onSuccess(method, response.body());
                    Log.i(TAG, "infura " + method + " onResponse " + response.code());
                } else {
                    infuraBodyListener.onFail(method, String.valueOf(response.code()));
                    Log.e(TAG, "infura " + method + " onResponse " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<InfuraResponse> call, @NonNull Throwable t) {
                infuraBodyListener.onFail(method, String.valueOf(t.getMessage()));
                Log.e(TAG, "infura " + method + " onFailure " + t.getMessage());
            }
        });
    }

    /**
     * HTTP
     * Card verify
     */
    private CardVerifyAndGetInfoListener cardVerifyAndGetInfoListener;

    public interface CardVerifyAndGetInfoListener {
        void onSuccess(CardVerifyAndGetInfo.Response cardVerifyAndGetArtworkResponse);
        void onFail(String message);
    }

    public void setCardVerifyAndGetInfoListener(CardVerifyAndGetInfoListener listener) {
        cardVerifyAndGetInfoListener = listener;
    }

    public void cardVerifyAndGetInfo(TangemCard card) {
        TangemApi tangemApi = App.getNetworkComponent().getRetrofitTangem().create(TangemApi.class);

        List<CardVerifyAndGetInfo.Request.Item> requests = new ArrayList<>();
        requests.add(new CardVerifyAndGetInfo.Request.Item(Util.bytesToHex(card.getCID()), Util.bytesToHex(card.getCardPublicKey())));

        CardVerifyAndGetInfo.Request requestBody = new CardVerifyAndGetInfo.Request(requests);

        Call<CardVerifyAndGetInfo.Response> call = tangemApi.getCardVerifyAndGetInfo(requestBody);
        call.enqueue(new Callback<CardVerifyAndGetInfo.Response>() {
            @Override
            public void onResponse(@NonNull Call<CardVerifyAndGetInfo.Response> call, @NonNull Response<CardVerifyAndGetInfo.Response> response) {
                if (response.code() == 200) {
                    CardVerifyAndGetInfo.Response cardVerifyAndGetArtworkResponse = response.body();
                    cardVerifyAndGetInfoListener.onSuccess(cardVerifyAndGetArtworkResponse);
                    Log.i(TAG, "cardVerifyAndGeInfo onResponse " + response.code());
                } else {
                    cardVerifyAndGetInfoListener.onFail(String.valueOf(response.code()));
                    Log.e(TAG, "cardVerifyAndGetInfo onResponse " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CardVerifyAndGetInfo.Response> call, @NonNull Throwable t) {
                cardVerifyAndGetInfoListener.onFail(t.getMessage());
                Log.e(TAG, "cardVerifyAndGetInfo onFailure " + t.getMessage());
            }
        });
    }

    /**
     * HTTP
     * Last version request from GitHub
     */
    private ArtworkListener artworkListener;

    public interface ArtworkListener {
        void onSuccess(String artworkId, InputStream inputStream, Date updateDate);
        void onFail(String message);
    }

    public void setArtworkListener(ArtworkListener listener) {
        artworkListener = listener;
    }

    public void requestArtwork(String artworkId, Date updateDate, TangemCard card) {
        TangemApi tangemApi = App.getNetworkComponent().getRetrofitTangem().create(TangemApi.class);

        Call<ResponseBody> call = tangemApi.getArtwork(artworkId, Util.bytesToHex(card.getCID()), Util.bytesToHex(card.getCardPublicKey()));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "getArtwork onResponse " + response.code());
                if (response.code() == 200) {
                    try {
                        ResponseBody body = response.body();
                        if (body != null) {
                            artworkListener.onSuccess(artworkId, body.byteStream(), updateDate);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                artworkListener.onFail(t.getMessage());
                Log.e(TAG, "getArtwork onFailure " + t.getMessage());
            }
        });
    }

    /**
     * HTTP
     * Used in Crypto-currency course
     */
    private RateInfoDataListener rateInfoDataListener;

    public interface RateInfoDataListener {
        void onSuccess(RateInfoResponse rateInfoResponse);
    }

    public void setRateInfoData(RateInfoDataListener listener) {
        rateInfoDataListener = listener;
    }

    @SuppressLint("CheckResult")
    public void rateInfoData(String cryptoId) {
        CoinmarketApi coinmarketApi = App.getNetworkComponent().getRetrofitCoinmarketcap().create(CoinmarketApi.class);

        coinmarketApi.getRateInfoList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rateInfoModelList -> {
                            if (!rateInfoModelList.isEmpty()) {
                                for (RateInfoResponse rateInfoMode : rateInfoModelList) {
                                    if (rateInfoMode.getId().equals(cryptoId)) {
                                        rateInfoDataListener.onSuccess(rateInfoMode);
                                        Log.i(TAG, "rateInfoData        " + cryptoId + " onResponse " + "200");
                                    }
                                }
                            } else
                                Log.e(TAG, "rateInfoData        " + cryptoId + " onResponse " + "Empty");
                        },
                        // handle error
                        Throwable::printStackTrace);
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
        UpdateVersionApi updateVersionApi = App.getNetworkComponent().getRetrofitGithubusercontent().create(UpdateVersionApi.class);

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