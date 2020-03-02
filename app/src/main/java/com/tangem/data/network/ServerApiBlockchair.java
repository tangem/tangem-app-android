package com.tangem.data.network;

import com.tangem.App;
import com.tangem.data.Blockchain;
import com.tangem.data.network.model.BlockchairAddressResponse;
import com.tangem.data.network.model.BlockchairSendBody;
import com.tangem.data.network.model.BlockchairStatsResponse;
import com.tangem.data.network.model.BlockchairTransactionResponse;
import com.tangem.tangem_card.util.Log;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ServerApiBlockchair {
    private static String TAG = ServerApiBlockchair.class.getSimpleName();
    private String blockchain;

    public ServerApiBlockchair(Blockchain blockchain) {
        if (blockchain == Blockchain.BitcoinCash) this.blockchain = "bitcoin-cash";
    }

    public void getAddress(String wallet, SingleObserver<BlockchairAddressResponse> addressObserver) {
        Log.i(TAG, "new getAddress request");
        BlockchairApi api = App.Companion.getNetworkComponent().getRetrofitBlockchair().create(BlockchairApi.class);

        Single<BlockchairAddressResponse> addressSingle = api.getAddress(blockchain, wallet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        addressSingle.subscribe(addressObserver);
    }

    public void getTransaction(String transaction, SingleObserver<BlockchairTransactionResponse> transactionObserver) {
        Log.i(TAG, "new getAddress request");
        BlockchairApi api = App.Companion.getNetworkComponent().getRetrofitBlockchair().create(BlockchairApi.class);

        Single<BlockchairTransactionResponse> transactionSingle = api.getTransaction(blockchain, transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        transactionSingle.subscribe(transactionObserver);
    }

    public void getStats(SingleObserver<BlockchairStatsResponse> statsObserver) {
        Log.i(TAG, "new getStats request");
        BlockchairApi api = App.Companion.getNetworkComponent().getRetrofitBlockchair().create(BlockchairApi.class);

        Single<BlockchairStatsResponse> statsSingle = api.getStats(blockchain)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        statsSingle.subscribe(statsObserver);
    }

    public void sendTransaction(String tx, CompletableObserver sendObserver) {
        Log.i(TAG, "new getAddress request");
        BlockchairApi api = App.Companion.getNetworkComponent().getRetrofitDucatus().create(BlockchairApi.class);

        Completable sendCompletable = api.sendTransaction(blockchain, new BlockchairSendBody(tx))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        sendCompletable.subscribe(sendObserver);
    }

    public String getUrl() {
        return ServerURL.API_BLOCKCHAIR;
    }
}
