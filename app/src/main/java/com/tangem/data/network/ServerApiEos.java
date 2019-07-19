package com.tangem.data.network;

import android.util.Log;

import com.tangem.wallet.eos.EosApiPush;
import com.tangem.wallet.eos.EosPushTransactionRequest;

import io.jafka.jeos.EosApi;
import io.jafka.jeos.EosApiFactory;
import io.jafka.jeos.core.request.chain.transaction.PushTransactionRequest;
import io.jafka.jeos.core.response.chain.account.Account;
import io.jafka.jeos.core.response.chain.transaction.PushedTransaction;
import io.jafka.jeos.impl.EosApiServiceGenerator;
import io.jafka.jeos.impl.EosChainApiService;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ServerApiEos {
    private static String TAG = ServerApiBinance.class.getSimpleName();

    public static void getBalance(String wallet, Observer<Account> accountObserver) {
        Log.i(TAG, "new getBalance request");
        EosApi eosApi = EosApiFactory.create("https://api.eosdetroit.io:443"); //TODO: add random server request

        Observable<Account> accountObservable = Observable.just(new Account())
                .map(account -> eosApi.getAccount(wallet))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        accountObservable.subscribe(accountObserver);
    }

    public static void sendTransaction(EosPushTransactionRequest req, Observer<PushedTransaction> sendObserver) {
        Log.i(TAG, "new getBalance request");
//        EosApi eosApi = EosApiFactory.create("https://api.eosdetroit.io:443"); //TODO: add random server request
        EosApiPush eosApiPush = EosApiServiceGenerator.createService(EosApiPush.class, "https://api.eosdetroit.io:443"); //TODO: add random server request

        Observable<PushedTransaction> sendObservable = Observable.just(new PushedTransaction())
                .map(pushedTransaction -> EosApiServiceGenerator.executeSync(eosApiPush.pushTransaction(req)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        sendObservable.subscribe(sendObserver);
    }

}
