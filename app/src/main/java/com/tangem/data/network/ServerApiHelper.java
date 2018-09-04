package com.tangem.data.network;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tangem.data.network.model.CardVerify;
import com.tangem.data.network.model.CardVerifyBody;
import com.tangem.data.network.model.CardVerifyResponse;
import com.tangem.data.network.model.InfuraEthGasPriceBody;
import com.tangem.data.network.model.InfuraEthGasPriceResponse;
import com.tangem.data.network.model.RateInfoResponse;
import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.domain.BitcoinNode;
import com.tangem.domain.BitcoinNodeTestNet;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServerApiHelper {
    private static String TAG = ServerApiHelper.class.getSimpleName();

    /**
     * HTTP
     * InfuraEthGasPrice
     */
    private InfuraEthGasPriceBodyListener infuraEthGasPriceBodyListener;

    public interface InfuraEthGasPriceBodyListener {
        void onInfuraEthGasPrice(InfuraEthGasPriceResponse cardVerifyResponse);
    }

    public void setInfuraEthGasPrice(InfuraEthGasPriceBodyListener listener) {
        infuraEthGasPriceBodyListener = listener;
    }

    public void infuraEthGasPrice(String method, int id) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Server.ApiInfura.URL_INFURA)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        InfuraApi infuraApi = retrofit.create(InfuraApi.class);

        InfuraEthGasPriceBody infuraEthGasPriceBody = new InfuraEthGasPriceBody(method, id);

        Call<InfuraEthGasPriceResponse> call = infuraApi.ethGasPrice("application/json", infuraEthGasPriceBody);

        call.enqueue(new Callback<InfuraEthGasPriceResponse>() {
            @Override
            public void onResponse(@NonNull Call<InfuraEthGasPriceResponse> call, @NonNull Response<InfuraEthGasPriceResponse> response) {
                if (response.code() == 200) {
                    infuraEthGasPriceBodyListener.onInfuraEthGasPrice(response.body());
                    Log.i(TAG, "infuraEthGasPrice onResponse " + response.code());
                } else
                    Log.e(TAG, "infuraEthGasPrice onResponse " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<InfuraEthGasPriceResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "infuraEthGasPrice onFailure " + t.getMessage());
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

        Call<CardVerifyResponse> call = tangemApi.getCardVerify("application/json", cardVerifyBody);
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
                                        Log.i("rateInfoData", rateInfoMode.getId());
                                        Log.i("rateInfoData", rateInfoMode.getPriceUsd());
                                    }
                                }
                            }
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

}