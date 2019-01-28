package com.tangem.data.network;

import android.util.Log;

import com.tangem.App;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.data.Blockchain;
import com.tangem.domain.wallet.bch.BitcoinCashNode;
import com.tangem.domain.wallet.btc.BitcoinNode;
import com.tangem.domain.wallet.btc.BitcoinNodeTestNet;
import com.tangem.domain.wallet.ltc.LitecoinNode;
import com.tangem.wallet.R;

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

/**
 * Request processor for Electrum Api
 * Every request live cycle:
 * 1. In application create request and call {@link ServerApiElectrum}.requestData(..)
 * 2. Try send every request for max 4 times,
 * 3. If all 4 times fail call DefaultObserver<ElectrumRequest>.onError (defined in .requestData(..)) and than
 *    {@link ResponseListener}.onFail(...) callback
 *    Error can be acquired with {@link ElectrumRequest}.getError() method
 * 4. If request network communication finished successfully then call DefaultObserver<ElectrumRequest>.onComplete (defined in .requestData) and than
 *    {@link ResponseListener}.onSuccess(...) callback
 */
public class ServerApiElectrum {
    private static String TAG = ServerApiElectrum.class.getSimpleName();

    /**
     * TCP, SSL
     * Used in BTC, BCH
     */
    private ResponseListener responseListener;
    private String host;
    private int port;

    private int requestsCount=0;

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    /**
     * Interface for notification every request result
     */
    public interface ResponseListener {

        /**
         * Notify that request processing was successful
         * @param electrumRequest - processed request containing received answer {@see electrumRequest.getAnswer() method}
         */
        void onSuccess(ElectrumRequest electrumRequest);
        /**
         * Notify that request processing was successful
         * @param electrumRequest - processed request containing occurred error {@see electrumRequest.getError() method}
         */
        void onFail(ElectrumRequest electrumRequest);
    }

    /**
     * Set notificaion listener
     * @param listener
     */
    public void setResponseListener(ResponseListener listener) {
        responseListener = listener;
    }


    /**
     * Start process request
     * @param ctx
     * @param electrumRequest
     */
    public void requestData(TangemContext ctx, ElectrumRequest electrumRequest) {
        requestsCount++;
        Log.i(TAG, String.format("New request[%d]: %s", requestsCount,electrumRequest.getMethod()));
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
            //TODO remove onNext
            @Override
            public void onNext(ElectrumRequest v) {
                if (electrumRequest.answerData != null) {
                    Log.i(TAG, "requestData " + electrumRequest.getMethod() + " onNext != null");
                } else {
                    Log.e(TAG, "requestData " + electrumRequest.getMethod() + " onNext == null");
                }
            }

            @Override
            public void onError(Throwable e) {
                requestsCount--;
                Log.e(TAG, "requestData " + electrumRequest.getMethod() + " onError " + e.getMessage());
                Log.e(TAG, String.format("%d requests left in processing",requestsCount));
                electrumRequest.setError(ctx.getString(R.string.cannot_obtain_data_from_blockchain));
                //setErrorOccurred(e.getMessage());//;
                responseListener.onFail(electrumRequest);
            }

            /**
             * Called after completion request processing
             */
            @Override
            public void onComplete() {
                requestsCount--;
                if (electrumRequest.answerData != null) {
                    Log.i(TAG, "requestData " + electrumRequest.getMethod() + " onComplete, answerData!=null");
                } else {
                    Log.e(TAG, "requestData " + electrumRequest.getMethod() + " onComplete, answerData==null");
                }
                Log.e(TAG, String.format("%d requests left in processing",requestsCount));
                if (electrumRequest.answerData != null && electrumRequest.getError()==null) {
                    responseListener.onSuccess(electrumRequest);
                } else {
//                    if( error==null || error.isEmpty() ) setErrorOccurred(ctx.getString(R.string.cannot_obtain_data_from_blockchain));
                    responseListener.onFail(electrumRequest);
                }
            }

        });
    }

    private void doElectrumRequest(TangemContext ctx, ElectrumRequest electrumRequest) {
        String host;
        int port;
        String proto;
        electrumRequest.setError(null);
        // todo - get available URL list from coinEngine, remove if( ctx.getBlockchain()==...)
        if (ctx.getBlockchain() == Blockchain.BitcoinTestNet) {
            BitcoinNodeTestNet bitcoinNodeTestNet = BitcoinNodeTestNet.values()[new Random().nextInt(BitcoinNodeTestNet.values().length)];
            host = bitcoinNodeTestNet.getHost();
            port = bitcoinNodeTestNet.getPort();

            this.host = host;
            this.port = port;

            doElectrumRequestTcp(electrumRequest, host, port);
        } else if (ctx.getBlockchain() == Blockchain.BitcoinCash) {
            BitcoinCashNode bitcoinCashNode = BitcoinCashNode.values()[new Random().nextInt(BitcoinCashNode.values().length)];
            host = bitcoinCashNode.getHost();
            port = bitcoinCashNode.getPort();
            proto = bitcoinCashNode.getProto();

            this.host = host;
            this.port = port;

            if (proto.equals("tcp")) {
                doElectrumRequestTcp(electrumRequest, host, port);
            } else {
                doElectrumRequestSsl(electrumRequest, host, port);
            }

        } else if (ctx.getBlockchain() == Blockchain.Bitcoin) {
            BitcoinNode bitcoinNode = BitcoinNode.values()[new Random().nextInt(BitcoinNode.values().length)];
            host = bitcoinNode.getHost();
            port = bitcoinNode.getPort();
            proto = bitcoinNode.getProto();

            this.host = host;
            this.port = port;

            if (proto.equals("tcp")) {
                doElectrumRequestTcp(electrumRequest, host, port);
            } else {
                doElectrumRequestSsl(electrumRequest, host, port);
            }
        } else if (ctx.getBlockchain() == Blockchain.Litecoin) {
            LitecoinNode litecoinNode = LitecoinNode.values()[new Random().nextInt(LitecoinNode.values().length)];
            host = litecoinNode.getHost();
            port = litecoinNode.getPort();
            proto = litecoinNode.getProto();

            this.host = host;
            this.port = port;

            if (proto.equals("tcp")) {
                doElectrumRequestTcp(electrumRequest, host, port);
            } else {
                doElectrumRequestSsl(electrumRequest, host, port);
            }
        }
    }

    private void doElectrumRequestTcp(ElectrumRequest electrumRequest, String host, int port) {
        try {
            Socket socket = App.getNetworkComponent().getSocket();
            socket.setSoTimeout(3000);
            Log.i(TAG, "Start process "+electrumRequest.getMethod()+" @ "+host + ":" + port);
            socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));
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
                    electrumRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain_no_answer));
                    Log.i(TAG, ">> <NULL>");
                }

            } catch (ConnectException e) {
                //e.printStackTrace();
                //responseListener.onFail(e.getMessage());
                electrumRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain_no_connection));
                Log.e(TAG, "doElectrumRequestTcp " + electrumRequest.getMethod() + " ConnectException " + e.getMessage());
            } finally {
                Log.i(TAG, "doElectrumRequestTcp " + electrumRequest.getMethod() + " socket.close");
                try {
                    if( socket.isConnected() ) socket.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.e(TAG,"Can't close socket");
                    electrumRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain_communication_error));
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
            //responseListener.onFail(e.getMessage());
            electrumRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain_communication_error));
            Log.e(TAG, "doElectrumRequestTcp " + electrumRequest.getMethod() + " IOException " + e.getMessage());
        }
    }

    private void doElectrumRequestSsl(ElectrumRequest electrumRequest, String host, int port) {
        try {
            // create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {

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
                Log.i(TAG, host + " " + port);
                sslSocket = (SSLSocket) sf.createSocket(host, port);
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
                        electrumRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain_no_answer));
                        Log.i(TAG, ">> <NULL>");
                    }

                } catch (ConnectException e) {
                    e.printStackTrace();
                    electrumRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain_no_connection));
                    Log.e(TAG, "doElectrumRequestSsl " + electrumRequest.getMethod() + " ConnectException " + e.getMessage());
                } finally {
                    Log.i(TAG, "doElectrumRequestSsl " + electrumRequest.getMethod() + " socket.close");
                    try {
                        if( sslSocket.isConnected() ) sslSocket.close();
                    }
                    catch (Exception e)
                    {
                        electrumRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain_communication_error));
                        e.printStackTrace();
                        Log.e(TAG, "Can't close ssl socket");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                electrumRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain_communication_error));
                Log.e(TAG, "doElectrumRequestSsl " + electrumRequest.getMethod() + " IOException " + e.getMessage());
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            electrumRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain));
            Log.e(TAG, e.getMessage());
        }
    }

    public String getValidationNodeDescription() {
        return "Electrum, " + host + ":" + String.valueOf(port);
    }


}