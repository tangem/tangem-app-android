package com.tangem.data.network;

import android.util.Log;

import com.tangem.App;
import com.tangem.data.network.model.BlockchainInfoAddress;
import com.tangem.data.network.model.BlockchainInfoAddressAndUnspents;
import com.tangem.data.network.model.BlockchainInfoTransaction;
import com.tangem.data.network.model.BlockchainInfoUnspents;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class ServerApiBlockchainInfo {
    private static String TAG = ServerApiBlockchainInfo.class.getSimpleName();
    private int page = 1;

    public void getAddressAndUnspents(String wallet, SingleObserver<BlockchainInfoAddressAndUnspents> addressAndUnspentsObserver) {
        Log.i(TAG, "new getAddressAndUnspents request");
        BlockchainInfoApi api = App.Companion.getNetworkComponent().getRetrofitBlockchainInfo().create(BlockchainInfoApi.class);

        Single<BlockchainInfoAddress> addressObservable = api.blockchainInfoAddress(wallet, null);

        Single<BlockchainInfoUnspents> unspentsObservable = api.blockchainInfoUnspents(wallet)
                .onErrorReturnItem(new BlockchainInfoUnspents(new ArrayList<>()));

        Single.zip(addressObservable, unspentsObservable, BlockchainInfoAddressAndUnspents::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(addressAndUnspentsObserver);
    }

    public void sendTransaction(String tx, SingleObserver<ResponseBody> sendObserver) {
        Log.i(TAG, "new getAddress request");
        BlockchainInfoApi api = App.Companion.getNetworkComponent().getRetrofitBlockchainInfo().create(BlockchainInfoApi.class);

        Single<ResponseBody> sendObservable = api.blockchainInfoPush(tx)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        sendObservable.subscribe(sendObserver);
    }

    public Single<List<BlockchainInfoTransaction>> getMoreAddressTxs(String wallet) {
        Log.i(TAG, "new getAddress request");
        BlockchainInfoApi api = App.Companion.getNetworkComponent().getRetrofitBlockchainInfo().create(BlockchainInfoApi.class);

        Single<List<BlockchainInfoTransaction>> addressObservable = api.blockchainInfoAddress(wallet, page * 50)
                .map(BlockchainInfoAddress::getTxs)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        page++;
        return addressObservable;
    }
}
