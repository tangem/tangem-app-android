package com.tangem.data.network;

import android.annotation.SuppressLint;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tangem.data.network.model.RateInfoModel;
import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.domain.BitcoinNode;
import com.tangem.domain.BitcoinNodeTestNet;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.TangemCard;

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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServerApiHelper {
    private static String TAG = ServerApiHelper.class.getSimpleName();

    /**
     * HTTP
     * Used in Crypto-currency course
     */
    private RateInfoDataListener rateInfoDataListener;

    public interface RateInfoDataListener {
        void onRateInfoDataData(RateInfoModel rateInfoModel);
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
                                for (RateInfoModel rateInfoMode : rateInfoModelList) {
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