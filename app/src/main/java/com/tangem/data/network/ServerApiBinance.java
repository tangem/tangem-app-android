package com.tangem.data.network;

import android.util.Log;

import com.tangem.wallet.TangemContext;
import com.tangem.wallet.Transaction;
import com.tangem.wallet.binance.BinanceAssetData;
import com.tangem.wallet.binance.BinanceData;
import com.tangem.wallet.binance.client.BinanceDexApiRestClient;
import com.tangem.wallet.binance.client.domain.Account;
import com.tangem.wallet.binance.client.domain.Balance;
import com.tangem.wallet.binance.client.domain.TransactionMetadata;
import com.tangem.wallet.binance.client.encoding.message.TransactionRequestAssemblerExtSign;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.RequestBody;

public class ServerApiBinance {
    private static String TAG = ServerApiBinance.class.getSimpleName();

    private ResponseListener responseListener;

    public interface ResponseListener {
        void onSuccess();
        void onFail();
    }

    public void setResponseListener(ResponseListener listener) {
        responseListener = listener;
    }

    public void getBalance(TangemContext ctx, BinanceDexApiRestClient client) {
        Log.i(TAG, "new getBalance request");

        Observable<Account> balanceObservable = Observable.just(new Account())
                .map(account -> client.getAccount(ctx.getCoinData().getWallet()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        balanceObservable.subscribe(new DefaultObserver<Account>() {
            @Override
            public void onNext(Account account) {
                Log.i(TAG, "getBalance onNext");
//                account = client.getAccount(ctx.getCoinData().getWallet());
                BinanceData binanceData = (BinanceData) ctx.getCoinData();

                for (Balance balance : account.getBalances()) {
                    if (balance.getSymbol().equals("BNB")) {
                        binanceData.setBalanceReceived(true);
                        binanceData.setBalance(balance.getFree());
                        break;
                    }
                }

                if (binanceData instanceof BinanceAssetData) {
                    BinanceAssetData binanceAssetData = (BinanceAssetData) binanceData;
                    for (Balance balance : account.getBalances()) {
                        if (balance.getSymbol().equals(ctx.getCard().getContractAddress())) {
                            binanceAssetData.setAssetBalance(balance.getFree());
                            break;
                        }
                    }
                }

                if (!binanceData.isBalanceReceived()) {
                    binanceData.setBalanceReceived(true);
                    binanceData.setBalance("0");
                }

                binanceData.setAccountNumber(account.getAccountNumber());
                binanceData.setSequence(account.getSequence());
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "getBalance onError" + e.getMessage());
                if (e.getMessage().contains("account not found")) {
                    ((BinanceData)ctx.getCoinData()).setError404(true);
                    responseListener.onFail();
                } else {
                    e.printStackTrace();
                    ctx.setError(e.getMessage());
                    responseListener.onFail();
                }
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "getBalance onComplete");
                responseListener.onSuccess();
            }
        });
    }

    public void sendTransaction (byte[] txForSend, BinanceDexApiRestClient client) {
        Log.i(TAG, "new sendTransaction request");

        RequestBody requestBody = TransactionRequestAssemblerExtSign.createRequestBody(txForSend);

        Observable<List<TransactionMetadata>> sendObservable = Observable.just(new ArrayList<>())
                .map(metadatas -> client.broadcastNoWallet(requestBody, true))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        sendObservable.subscribe(new DefaultObserver<List<TransactionMetadata>>() {
            @Override
            public void onNext(List<TransactionMetadata> metadatas ) {
//                RequestBody requestBody = TransactionRequestAssemblerExtSign.createRequestBody(txForSend);
//                List<TransactionMetadata> metadatas = client.broadcastNoWallet(requestBody, true);
                if (!metadatas.isEmpty() && metadatas.get(0).isOk()) {
                    responseListener.onSuccess();
                } else {
                    Log.e(TAG, "Transaction send error");
                    responseListener.onFail();
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "sendTransaction onError" + e.getMessage());
                e.printStackTrace();
                responseListener.onFail();
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "sendTransaction onComplete");
            }
        });
    }
}
