package com.tangem.data.network;

import com.tangem.App;
import com.tangem.data.Blockchain;
import com.tangem.util.LOG;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;

import org.stellar.sdk.Network;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.ErrorResponse;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by dvol on 7.01.2019.
 * <p>
 * Request processor for Stellar Horizon Rest Api
 * Every request live cycle:
 * 1. In application create request and call {@link ServerApiStellar}.requestData(..)
 * 2. Try send every request for max 4 times,
 * 3. If all 4 times fail call DefaultObserver<StellarRequest>.onError (defined in .requestData(..)) and than
 * {@link Listener}.onFail(...) callback
 * Error can be acquired with {@link StellarRequest}.getError() method
 * 4. If request network communication finished successfully then call DefaultObserver<StellarRequest.Base>.onComplete (defined in .requestData) and than
 * {@link Listener}.onSuccess(...) callback
 */
public class ServerApiStellar {

    public ServerApiStellar(Blockchain blockchain) {
        if (blockchain == Blockchain.Stellar || blockchain == Blockchain.StellarAsset || blockchain == Blockchain.StellarTag) {
            currentURL = ServerURL.API_STELLAR;
        } else {
            currentURL = ServerURL.API_STELLAR_TESTNET;
        }
    }

    private static String TAG = ServerApiStellar.class.getSimpleName();

    /**
     * TCP, SSL
     * Used in BTC, BCH
     */
    private Listener listener;

    private int requestsCount = 0;

    private String currentURL;

    public String getCurrentURL() {
        return currentURL;
    }

    public boolean isRequestsSequenceCompleted() {
        LOG.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    /**
     * Interface for notification every request result
     */
    public interface Listener {

        /**
         * Notify that request processing was successful
         *
         * @param stellarRequest - processed request containing received answer {@see stellarRequest.getAnswer() method}
         */
        void onSuccess(StellarRequest.Base stellarRequest);

        /**
         * Notify that request processing was successful
         *
         * @param stellarRequest - processed request containing occurred error {@see stellarRequest.getError() method}
         */
        void onFail(StellarRequest.Base stellarRequest);
    }

    /**
     * Set notificaion listener
     *
     * @param listener
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }


    /**
     * Start process request
     *
     * @param ctx
     * @param stellarRequest
     */

    public void requestData(TangemContext ctx, StellarRequest.Base stellarRequest) {
        requestData(ctx, stellarRequest, false);
    }

    public void requestData(TangemContext ctx, StellarRequest.Base stellarRequest, boolean isRetry) {
        requestsCount++;
        LOG.i(TAG, String.format("New request[%d]: %s", requestsCount, stellarRequest.getClass().getSimpleName()));

        Observable<StellarRequest.Base> stellarObserver = Observable.just(stellarRequest)
                .doOnEach(stellarRequest1 -> doStellarRequest(ctx, stellarRequest))

                .flatMap(stellarRequest1 -> {
                            if (stellarRequest1.errorResponse != null) {
                                LOG.e(TAG, "Error response on " + stellarRequest.getClass().getSimpleName());
                                return Observable.error(stellarRequest.errorResponse);
                            } else
                                return Observable.just(stellarRequest1);
                        }
                )
//                .retryWhen(errors -> errors
//                        .filter(throwable -> (throwable instanceof IOException) || (throwable instanceof ErrorResponse))
//                        .zipWith(Observable.range(1, 4), (n, i) -> i))

                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        stellarObserver.subscribe(new DefaultObserver<StellarRequest.Base>() {

            @Override
            public void onNext(StellarRequest.Base stellarRequest) {
                LOG.e(TAG, "requestData " + stellarRequest.getClass().getSimpleName() + " onNext ");
            }

            @Override
            public void onError(Throwable e) {
                requestsCount--;
                LOG.e(TAG, "requestData " + stellarRequest.getClass().getSimpleName() + " onError " + e.getMessage());
                LOG.e(TAG, String.format("%d requests left in processing", requestsCount));

                if (isRetry || (stellarRequest.errorResponse != null && stellarRequest.errorResponse.getCode() == 404)) {
                    stellarRequest.setError(e.getMessage());
                    //setErrorOccurred(e.getMessage());//;
                    listener.onFail(stellarRequest);
                } else {
                    retryRequest(ctx, stellarRequest);
                }
            }

            /**
             * Called after completion request processing
             */
            @Override
            public void onComplete() {
                requestsCount--;
                LOG.e(TAG, String.format("%d requests left in processing", requestsCount));
                if (stellarRequest.getError() != null) {
                    LOG.i(TAG, "requestData " + stellarRequest.getClass().getSimpleName() + " onComplete, error!=null");

                    if (isRetry || (stellarRequest.errorResponse != null && stellarRequest.errorResponse.getCode() == 404)) {
                        listener.onFail(stellarRequest);
                    } else {
                        retryRequest(ctx, stellarRequest);
                    }
                } else {
                    LOG.e(TAG, "requestData " + stellarRequest.getClass().getSimpleName() + " onComplete, error==null");
                    listener.onSuccess(stellarRequest);
                }
            }

        });
    }

    public void doStellarRequest(TangemContext ctx, StellarRequest.Base stellarRequest) throws IOException {
        stellarRequest.setError(null);
        try {
            Server server;
            Blockchain blockchain = ctx.getBlockchain();
            if (blockchain == Blockchain.Stellar || blockchain == Blockchain.StellarAsset || blockchain == Blockchain.StellarTag) {
                Network.usePublicNetwork();
                server = new Server(currentURL);
            } else if (blockchain == Blockchain.StellarTestNet) {
                Network.useTestNetwork();
                server = new Server(currentURL);
            } else {
                throw new IOException("Wrong blockchain for ServerApiStellar");
            }
            try {
                LOG.e(TAG, "--- request " + stellarRequest.getClass().getSimpleName());
                stellarRequest.process(server);
            } catch (ErrorResponse errorResponse) {
                LOG.e(TAG, "--- error response: " + errorResponse.getMessage());
                stellarRequest.errorResponse = errorResponse;
                stellarRequest.setError(errorResponse.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            stellarRequest.setError(App.Companion.getInstance().getString(R.string.loaded_wallet_error_blockchain_communication_error));
            throw e;
        }
    }

    public void retryRequest (TangemContext ctx, StellarRequest.Base stellarRequest) {
        currentURL = ServerURL.API_STELLAR_RESERVE;
        requestData(ctx, stellarRequest, true);
    }

}