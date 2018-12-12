package com.tangem.data.network;

import android.util.Log;

import com.tangem.App;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.data.Blockchain;
import com.tangem.domain.wallet.bch.BitcoinCashNode;
import com.tangem.domain.wallet.btc.BitcoinNode;
import com.tangem.domain.wallet.btc.BitcoinNodeTestNet;

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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;

public class ServerApiElectrum {
    private static String TAG = ServerApiElectrum.class.getSimpleName();

    /**
     * TCP, SSL
     * Used in BTC, BCH
     */
    private ElectrumRequestDataListener electrumRequestDataListener;
    private String host;
    private int port;

    private int requestsCount=0;

    public boolean hasRequests() {
        return requestsCount>0;
    }

    private String error=null;
    public boolean isErrorOccured() {
        return error!=null;
    }

    public void setErrorOccured(String error) {
        this.error=error;
    }

    public interface ElectrumRequestDataListener {
        void onSuccess(ElectrumRequest electrumRequest);

        void onFail(String method);
    }

    public void setElectrumRequestData(ElectrumRequestDataListener listener) {
        electrumRequestDataListener = listener;
    }

    public void electrumRequestData(TangemContext ctx, ElectrumRequest electrumRequest) {
        requestsCount++;
        Observable<ElectrumRequest> checkElectrumDataObserver = Observable.just(electrumRequest)
                .doOnNext(electrumRequest1 -> doElectrumRequest(ctx, electrumRequest))

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
                    requestsCount--;
                    electrumRequestDataListener.onSuccess(electrumRequest);
//                    Log.i(TAG, "electrumRequestData " + electrumRequest.getMethod() + " onNext != null");
                } else {
                    electrumRequestDataListener.onFail(electrumRequest.getMethod());
                    Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " onNext == null");
                }
            }

            @Override
            public void onError(Throwable e) {
                electrumRequestDataListener.onFail(electrumRequest.getMethod());
                Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " onError " + e.getMessage());
            }

            @Override
            public void onComplete() {
//                Log.i(TAG, "electrumRequestData " + electrumRequest.getMethod() + " onComplete");
            }

        });
    }

    private List<ElectrumRequest> doElectrumRequest(TangemContext ctx, ElectrumRequest electrumRequest) {
        String host;
        int port;
        String proto;
        if (ctx.getBlockchain() == Blockchain.BitcoinTestNet) {
            BitcoinNodeTestNet bitcoinNodeTestNet = BitcoinNodeTestNet.values()[new Random().nextInt(BitcoinNodeTestNet.values().length)];
            host = bitcoinNodeTestNet.getHost();
            port = bitcoinNodeTestNet.getPort();

            this.host = host;
            this.port = port;

            return doElectrumRequestTcp(electrumRequest, host, port);

        } else if (ctx.getBlockchain() == Blockchain.BitcoinCash) {
            BitcoinCashNode bitcoinCashNode = BitcoinCashNode.values()[new Random().nextInt(BitcoinCashNode.values().length)];
            host = bitcoinCashNode.getHost();
            port = bitcoinCashNode.getPort();
            proto = bitcoinCashNode.getProto();

            this.host = host;
            this.port = port;

            if (proto.equals("tcp")) {
                return doElectrumRequestTcp(electrumRequest, host, port);
            } else {
                return doElectrumRequestSsl(electrumRequest, host, port);
            }

        } else if (ctx.getBlockchain() == Blockchain.Bitcoin) {
            BitcoinNode bitcoinNode = BitcoinNode.values()[new Random().nextInt(BitcoinNode.values().length)];
            host = bitcoinNode.getHost();
            port = bitcoinNode.getPort();
            proto = bitcoinNode.getProto();

            this.host = host;
            this.port = port;

            if (proto.equals("tcp")) {
                return doElectrumRequestTcp(electrumRequest, host, port);
            } else {
                return doElectrumRequestSsl(electrumRequest, host, port);
            }
        }
        return null;
    }

    private List<ElectrumRequest> doElectrumRequestTcp(ElectrumRequest electrumRequest, String host, int port) {
        List<ElectrumRequest> result = new ArrayList<>();
        Collections.addAll(result, electrumRequest);

        try {
            Socket socket = App.getNetworkComponent().getSocket();
            socket.setSoTimeout(3000);
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
                    Log.i(TAG, ">> " + electrumRequest.answerData);
                } else {
                    electrumRequest.error = "No answer from server";
                    Log.i(TAG, ">> <NULL>");
                }

            } catch (ConnectException e) {
                e.printStackTrace();
                electrumRequestDataListener.onFail(e.getMessage());
                Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " ConnectException " + e.getMessage());
            } finally {
                Log.i(TAG, "electrumRequestData " + electrumRequest.getMethod() + " CLOSE");
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            electrumRequestDataListener.onFail(e.getMessage());
            Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " IOException " + e.getMessage());
        }
        return result;
    }

    private List<ElectrumRequest> doElectrumRequestSsl(ElectrumRequest electrumRequest, String host, int port) {
        try {
            // create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};

            // install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sf = sc.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(sf);

            // create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            SSLSocket sslSocket;

            List<ElectrumRequest> result = new ArrayList<>();
            Collections.addAll(result, electrumRequest);

            try {
                sslSocket = (SSLSocket) sf.createSocket(host, port);
                Log.i(TAG, host + " " + port);
                try {
                    OutputStream os = sslSocket.getOutputStream();
                    OutputStreamWriter out = new OutputStreamWriter(os, "UTF-8");
                    InputStream is = sslSocket.getInputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(is));
                    electrumRequest.setID(1);

                    out.write(electrumRequest.getAsString() + "\n");
                    out.flush();

                    electrumRequest.answerData = in.readLine();
                    if (electrumRequest.answerData != null) {
                        Log.i(TAG, ">> " + electrumRequest.answerData);
                    } else {
                        electrumRequest.error = "No answer from server";
                        Log.i(TAG, ">> <NULL>");
                    }

                } catch (ConnectException e) {
                    e.printStackTrace();
                    Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " ConnectException " + e.getMessage());
                } finally {
                    Log.i(TAG, "electrumRequestData " + electrumRequest.getMethod() + " CLOSE");
                    sslSocket.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "electrumRequestData " + electrumRequest.getMethod() + " IOException " + e.getMessage());
            }

            return result;

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }

    public String getValidationNodeDescription() {
        return "Electrum, " + host + ":" + String.valueOf(port);
    }


}