package com.tangem.data.network;

import android.util.Log;

import com.tangem.wallet.CoinEngine;

import io.jafka.jeos.EosApi;
import io.jafka.jeos.EosApiFactory;
import io.jafka.jeos.core.response.chain.account.Account;
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

}
