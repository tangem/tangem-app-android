package com.tangem.data.network;

import android.util.Log;

import com.tangem.App;
import com.tangem.data.Blockchain;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.wallet.R;

import org.stellar.sdk.Network;
import org.stellar.sdk.Server;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Request processor for Electrum Api
 * Every request live cycle:
 * 1. In application create request and call {@link ServerApiStellar}.requestData(..)
 * 2. Try send every request for max 4 times,
 * 3. If all 4 times fail call DefaultObserver<StellarRequest>.onError (defined in .requestData(..)) and than
 *    {@link Listener}.onFail(...) callback
 *    Error can be acquired with {@link StellarRequest}.getError() method
 * 4. If request network communication finished successfully then call DefaultObserver<StellarRequest>.onComplete (defined in .requestData) and than
 *    {@link Listener}.onSuccess(...) callback
 */
public class ServerApiStellar {
    private static String TAG = ServerApiStellar.class.getSimpleName();

    /**
     * TCP, SSL
     * Used in BTC, BCH
     */
    private Listener listener;

    private int requestsCount=0;

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    /**
     * Interface for notification every request result
     */
    public interface Listener {

        /**
         * Notify that request processing was successful
         * @param stellarRequest - processed request containing received answer {@see stellarRequest.getAnswer() method}
         */
        void onSuccess(StellarRequest.Base stellarRequest);
        /**
         * Notify that request processing was successful
         * @param stellarRequest - processed request containing occurred error {@see stellarRequest.getError() method}
         */
        void onFail(StellarRequest.Base stellarRequest);
    }

    /**
     * Set notificaion listener
     * @param listener
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }


    /**
     * Start process request
     * @param ctx
     * @param stellarRequest
     */
    public void requestData(TangemContext ctx, StellarRequest.Base stellarRequest) {
        requestsCount++;
        Log.i(TAG, String.format("New request[%d]: %s", requestsCount,stellarRequest.getClass().getSimpleName()));
        Observable<StellarRequest.Base> stellarObserver = Observable.just(stellarRequest)
                .doOnNext(stellarRequest1 -> doStellarRequest(ctx, stellarRequest))

//                .flatMap(stellarRequest1 -> {
//                    if (stellarRequest1.answerData == null) {
//                        Log.e(TAG, "NullPointerException " + stellarRequest.getMethod());
//                        return Observable.error(new NullPointerException());
//                    } else
//                        return Observable.just(stellarRequest1);
//                })
//
                .retryWhen(errors -> errors
                        .filter(throwable -> throwable instanceof IOException)
                        .zipWith(Observable.range(1, 4), (n, i) -> i))

                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        stellarObserver.subscribe(new DefaultObserver<StellarRequest.Base>() {

            @Override
            public void onNext(StellarRequest.Base stellarRequest) {

            }

            @Override
            public void onError(Throwable e) {
                requestsCount--;
                Log.e(TAG, "requestData " + stellarRequest.getClass().getSimpleName() + " onError " + e.getMessage());
                Log.e(TAG, String.format("%d requests left in processing",requestsCount));
                stellarRequest.setError(ctx.getString(R.string.cannot_obtain_data_from_blockchain));
                //setErrorOccurred(e.getMessage());//;
                listener.onFail(stellarRequest);
            }

            /**
             * Called after completion request processing
             */
            @Override
            public void onComplete() {
                requestsCount--;
                Log.e(TAG, String.format("%d requests left in processing",requestsCount));
                if (stellarRequest.getError() != null) {
                    Log.i(TAG, "requestData " + stellarRequest.getClass().getSimpleName() + " onComplete, error!=null");
                    listener.onFail(stellarRequest);
                } else {
                    Log.e(TAG, "requestData " + stellarRequest.getClass().getSimpleName() + " onComplete, error==null");
                    listener.onSuccess(stellarRequest);
                }
            }

        });
    }

    private void doStellarRequest(TangemContext ctx, StellarRequest.Base stellarRequest) throws IOException {
        stellarRequest.setError(null);
        try {
            if( ctx.getBlockchain()==Blockchain.StellarTestNet ) {
                Network.useTestNetwork();
            }
            Server server = new Server(ServerURL.API_STELLAR);
            stellarRequest.process(server);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            stellarRequest.setError(App.getInstance().getString(R.string.cannot_obtain_data_from_blockchain_communication_error));
            throw e;
        }
    }

}