package com.tangem.data.network;

import com.tangem.App;
import com.tangem.data.network.model.BitcoreBalance;
import com.tangem.data.network.model.BitcoreBalanceAndUnspents;
import com.tangem.data.network.model.BitcoreSendBody;
import com.tangem.data.network.model.BitcoreSendResponse;
import com.tangem.data.network.model.BitcoreUtxo;
import com.tangem.tangem_card.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ServerApiBitcore {
    private static String TAG = ServerApiBitcore.class.getSimpleName();

    public void getBalanceAndUnspents(String wallet, SingleObserver<BitcoreBalanceAndUnspents> balanceAndUnspentsObserver) {
        Log.i(TAG, "new getAddressAndUnspents request");
        DucatusApi api = App.Companion.getNetworkComponent().getRetrofitDucatus().create(DucatusApi.class);

        Single<BitcoreBalance> balanceObservable = api.ducatusBalance(wallet);

        Single<List<BitcoreUtxo>> unspentsObservable = api.ducatusUnspents(wallet)
                .onErrorReturnItem(new ArrayList<>());

        Single.zip(balanceObservable, unspentsObservable, BitcoreBalanceAndUnspents::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(balanceAndUnspentsObserver);
    }

    public void sendTransaction(String tx, SingleObserver<BitcoreSendResponse> sendObserver) {
        Log.i(TAG, "new getAddress request");
        DucatusApi api = App.Companion.getNetworkComponent().getRetrofitDucatus().create(DucatusApi.class);

        Single<BitcoreSendResponse> sendObservable = api.ducatusSend(new BitcoreSendBody(tx))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        sendObservable.subscribe(sendObserver);
    }
}
