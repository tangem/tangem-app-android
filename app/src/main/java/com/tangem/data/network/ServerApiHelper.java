package com.tangem.data.network;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tangem.data.network.model.CardVerify;
import com.tangem.data.network.model.CardVerifyBody;
import com.tangem.data.network.model.CardVerifyResponse;
import com.tangem.data.network.model.InfuraBody;
import com.tangem.data.network.model.InfuraResponse;
import com.tangem.data.network.model.RateInfoResponse;
import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.domain.BitcoinNode;
import com.tangem.domain.BitcoinNodeTestNet;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
        void onInfuraEthGasPrice(int blockCount, String estimateFeeResponse);
    }

    public void setEstimateFee(EstimateFeeListener listener) {
        estimateFeeListener = listener;
    }

    public void estimateFee(int blockCount) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Server.ApiEstimatefee.URL_ESTIMATEFEE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        EstimatefeeApi estimatefeeApi = retrofit.create(EstimatefeeApi.class);

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
                    estimateFeeListener.onInfuraEthGasPrice(blockCount, response.body());
                    Log.i(TAG, "estimateFee onResponse " + response.code());
                } else
                    Log.e(TAG, "estimateFee onResponse " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
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
     * eth_gasPrice
     */
    public static final String INFURA_ETH_GET_BALANCE = "eth_getBalance";
    public static final String INFURA_ETH_GET_TRANSACTION_COUNT = "eth_getTransactionCount";
    public static final String INFURA_ETH_CALL = "eth_call";
    public static final String INFURA_ETH_GAS_PRICE = "eth_gasPrice";
    private InfuraBodyListener infuraBodyListener;

    public interface InfuraBodyListener {
        void onInfura(String method, InfuraResponse infuraResponse);
    }

    public void setInfura(InfuraBodyListener listener) {
        infuraBodyListener = listener;
    }

    public void infura(String method, int id, String wallet, String contract) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Server.ApiInfura.URL_INFURA)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        InfuraApi infuraApi = retrofit.create(InfuraApi.class);

        InfuraBody infuraBody;
        switch (method) {
            case INFURA_ETH_GET_BALANCE:
            case INFURA_ETH_GET_TRANSACTION_COUNT:
                infuraBody = new InfuraBody(method, new String[]{wallet, "latest"}, id);
                break;

            case INFURA_ETH_CALL:
                String address = wallet.substring(2);
                infuraBody = new InfuraBody(method, new Object[]{new InfuraBody.EthCallParams("0x70a08231000000000000000000000000" + address, contract), "latest"}, id);
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
                    infuraBodyListener.onInfura(method, response.body());
                    Log.i(TAG, "infura " + method + " onResponse " + response.code());
                } else
                    Log.e(TAG, "infura " + method + " onResponse " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<InfuraResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "infura " + method + " onFailure " + t.getMessage());
            }
        });
    }

    /**
     * HTTP
     * Card verify
     */
    private CardVerifyListener cardVerifyListener;

    public interface CardVerifyListener {
        void onCardVerify(CardVerifyResponse cardVerifyResponse);
    }

    public void setCardVerify(CardVerifyListener listener) {
        cardVerifyListener = listener;
    }

    public void cardVerify(TangemCard card) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Server.ApiTangem.URL_TANGEM)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TangemApi tangemApi = retrofit.create(TangemApi.class);

        CardVerify[] requests = new CardVerify[1];
        requests[0] = new CardVerify(Util.bytesToHex(card.getCID()), Util.bytesToHex(card.getCardPublicKey()));

        CardVerifyBody cardVerifyBody = new CardVerifyBody(requests);

        Call<CardVerifyResponse> call = tangemApi.getCardVerify(cardVerifyBody);
        call.enqueue(new Callback<CardVerifyResponse>() {
            @Override
            public void onResponse(@NonNull Call<CardVerifyResponse> call, @NonNull Response<CardVerifyResponse> response) {
                if (response.code() == 200) {
                    CardVerifyResponse cardVerifyResponse = response.body();
                    cardVerifyListener.onCardVerify(cardVerifyResponse);
                    Log.i(TAG, "cardVerify onResponse " + response.code());
                } else
                    Log.e(TAG, "cardVerify onResponse " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<CardVerifyResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "cardVerify onFailure " + t.getMessage());
            }
        });
    }

    /**
     * HTTP
     * Used in Crypto-currency course
     */
    private RateInfoDataListener rateInfoDataListener;

    public interface RateInfoDataListener {
        void onRateInfoDataData(RateInfoResponse rateInfoResponse);
    }

    public void setRateInfoData(RateInfoDataListener listener) {
        rateInfoDataListener = listener;
    }

    @SuppressLint("CheckResult")
    public void rateInfoData(String cryptoId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Server.ApiCoinmarket.URL_COINMARKET)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        CoinmarketApi coinmarketApi = retrofit.create(CoinmarketApi.class);

        coinmarketApi.getRateInfoList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rateInfoModelList -> {
                            if (!rateInfoModelList.isEmpty()) {
                                for (RateInfoResponse rateInfoMode : rateInfoModelList) {
                                    if (rateInfoMode.getId().equals(cryptoId)) {
                                        rateInfoDataListener.onRateInfoDataData(rateInfoMode);
                                        Log.i(TAG, "rateInfoData " + cryptoId + " onResponse " + "200");
                                    }
                                }
                            } else
                                Log.e(TAG, "rateInfoData " + cryptoId + " onResponse " + "Empty");

                        },
                        // handle error
                        Throwable::printStackTrace);
    }

    /**
     * TCP
     * Used in BTC
     */
    private String host;
    private int port;
    private ElectrumRequestDataListener electrumRequestDataListener;

    public interface ElectrumRequestDataListener {
        void onElectrumRequestData(ElectrumRequest electrumRequest);
    }

    public void setElectrumRequestData(ElectrumRequestDataListener listener) {
        electrumRequestDataListener = listener;
    }

    public void electrumRequestData(TangemCard card, ElectrumRequest electrumRequest) {
        Observable<ElectrumRequest> checkBalanceObserver = Observable.just(electrumRequest)
                .doOnNext(electrumRequest1 -> doElectrumRequest(card, electrumRequest))
                .flatMap(electrumRequest1 -> {
                    if (electrumRequest1.answerData == null)
                        return Observable.error(new NullPointerException());
                    else
                        return Observable.just(electrumRequest1);
                })
                .retryWhen(errors -> errors
                        .filter(throwable -> throwable instanceof NullPointerException)
                        .zipWith(Observable.range(1, 2), (n, i) -> i))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        checkBalanceObserver.subscribe(new DefaultObserver<ElectrumRequest>() {
            @Override
            public void onNext(ElectrumRequest electrumRequest) {
                if (electrumRequest.answerData != null) {
                    electrumRequestDataListener.onElectrumRequestData(electrumRequest);
                } else {
                    Log.i(TAG, "onNext ElectrumRequest == null");
                }
                Log.i(TAG, "onNext");
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "onError " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete");
            }
        });
    }

    private List<ElectrumRequest> doElectrumRequest(TangemCard card, ElectrumRequest electrumRequest) {

        BitcoinNode bitcoinNode = BitcoinNode.values()[new Random().nextInt(BitcoinNode.values().length)];

        if (card.getBlockchain() == Blockchain.BitcoinTestNet || card.getBlockchain() == Blockchain.BitcoinCashTestNet) {
            BitcoinNodeTestNet bitcoinNodeTestNet = BitcoinNodeTestNet.values()[new Random().nextInt(BitcoinNodeTestNet.values().length)];
            this.host = bitcoinNodeTestNet.getHost();
            this.port = bitcoinNodeTestNet.getPort();

        } else {
            this.host = bitcoinNode.getHost();
            this.port = bitcoinNode.getPort();
        }

        List<ElectrumRequest> result = new ArrayList<>();
        Collections.addAll(result, electrumRequest);

        try {
            Log.i(TAG, "host " + host + " !!!!!!!!!!!! " + "port " + String.valueOf(port));
            InetAddress serverAddress = InetAddress.getByName(host);
            Socket socket = new Socket();
            socket.setSoTimeout(5000);
            socket.bind(new InetSocketAddress(0));
            socket.connect(new InetSocketAddress(serverAddress, port));
            try {
                Log.i(TAG, "<< ");
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter out = new OutputStreamWriter(os, "UTF-8");
                InputStream is = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                electrumRequest.setID(1);

                // do request
                try {
                    out.write(electrumRequest.getAsString() + "\n");
                    out.flush();

                    electrumRequest.answerData = in.readLine();
                    electrumRequest.host = host;
                    electrumRequest.port = port;
                    if (electrumRequest.answerData != null) {
                        Log.i(TAG, ">> " + electrumRequest.answerData);
                    } else {
                        electrumRequest.error = "No answer from server";
                        Log.i(TAG, ">> <NULL>");
                    }
                } catch (Exception e) {
                    electrumRequest.error = e.toString();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, e.getMessage());
            } finally {
                Log.i(TAG, "close");
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getValidationNodeDescription() {
        return "Electrum, " + host + ":" + String.valueOf(port);
    }

    /**
     * HTTP
     * Last version request from GitHub
     */
    private LastVersionListener lastVersionListener;

    public interface LastVersionListener {
        void onLastVersion(String lastVersion);
    }

    public void setLastVersionListener(LastVersionListener listener) {
        lastVersionListener = listener;
    }

    public void requestLastVersion() {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder().
                addInterceptor(logging).
//        addInterceptor(new AuthorizationInterceptor()).
        build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Server.ApiUpdateVersion.URL_UPDATE_VERSION)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        UpdateVersionApi updateVersionApi = retrofit.create(UpdateVersionApi.class);

        Call<ResponseBody> call = updateVersionApi.getLastVersion();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "lastVersion onResponse " + response.code());
                if (response.code() == 200) {
                    String stringResponse;
                    try {
                        stringResponse = response.body().string();
                        lastVersionListener.onLastVersion(stringResponse);
                    } catch (IOException e) {
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