package com.tangem.data.network;

import android.util.Log;

import com.tangem.App;
import com.tangem.domain.BitcoinNode;
import com.tangem.domain.BitcoinNodeTestNet;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.TangemCard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;

public class ServerApiHelperElectrum {
    //    private static String TAG = ServerApiHelper.class.getSimpleName();
    private static String TAG = ServerApiHelperElectrum.class.getSimpleName();

    /**
     * TCP
     * Used in BTC
     */
    private String host;
    private int port;
    private ElectrumRequestDataListener electrumRequestDataListener;

    public interface ElectrumRequestDataListener {
        void onElectrumSuccess(ElectrumRequest electrumRequest);

        void onElectrumFail(String method);
    }

    public void setElectrumRequestData(ElectrumRequestDataListener listener) {
        electrumRequestDataListener = listener;
    }

    public void electrumRequestData(TangemCard card, ElectrumRequest electrumRequest) {
        Observable<ElectrumRequest> checkElectrumDataObserver = Observable.just(electrumRequest)

                .doOnNext(electrumRequest1 -> doElectrumRequest(card, electrumRequest))

                .flatMap(electrumRequest1 -> {
                    if (electrumRequest1.answerData == null) {
                        Log.e(TAG, "NullPointerException " + electrumRequest.getMethod());
                        return Observable.error(new NullPointerException());
                    } else
                        return Observable.just(electrumRequest1);
                })

                .retryWhen(errors -> errors
                        .filter(throwable -> throwable instanceof NullPointerException)
                        .zipWith(Observable.range(1, 4), (n, i) -> i))


                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        checkElectrumDataObserver.subscribe(new DefaultObserver<ElectrumRequest>() {
            @Override
            public void onNext(ElectrumRequest v) {
                if (electrumRequest.answerData != null) {
                    electrumRequestDataListener.onElectrumSuccess(electrumRequest);
//                    Log.i(TAG, "electrumRequestData " + electrumRequest.getMethod() + " onNext != null");
                } else {
//                    electrumRequestDataListener.onElectrumFail(electrumRequest.getMethod());
                    Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " onNext == null");
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " onError " + e.getMessage());
            }

            @Override
            public void onComplete() {
//                Log.i(TAG, "electrumRequestData " + electrumRequest.getMethod() + " onComplete");
            }

        });
    }

    private List<ElectrumRequest> doElectrumRequest(TangemCard card, ElectrumRequest electrumRequest) throws ConnectException {
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
            Socket socket = App.getComponent().getSocket();
            socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));
            Log.i(TAG, host + " " + port);
            try {
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter out = new OutputStreamWriter(os, "UTF-8");
                InputStream is = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                electrumRequest.setID(1);

                out.write(electrumRequest.getAsString() + "\n");
                out.flush();

                electrumRequest.answerData = in.readLine();
                electrumRequest.host = host;
                electrumRequest.port = port;
                if (electrumRequest.answerData != null) {
//                        Log.i(TAG, ">> " + electrumRequest.answerData);
                } else {
                    electrumRequest.error = "No answer from server";
//                        Log.i(TAG, ">> <NULL>");
                }

            } catch (ConnectException e) {
                e.printStackTrace();
                Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " ConnectException " + e.getMessage());
            } finally {
                Log.i(TAG, "electrumRequestData " + electrumRequest.getMethod() + " CLOSE");
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            electrumRequestDataListener.onElectrumFail(electrumRequest.getMethod());
            Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " Exception " + e.getMessage());
        }
        return result;
    }

    public String getValidationNodeDescription() {
        return "Electrum, " + host + ":" + String.valueOf(port);
    }

}