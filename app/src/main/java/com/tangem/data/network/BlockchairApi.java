package com.tangem.data.network;

import com.tangem.data.network.model.BlockchairAddressResponse;
import com.tangem.data.network.model.BlockchairSendBody;
import com.tangem.data.network.model.BlockchairStatsResponse;
import com.tangem.data.network.model.BlockchairTransactionResponse;

import io.reactivex.Completable;
import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface BlockchairApi {
    @GET(Server.ApiBlockchair.Method.ADDRESS)
    Single<BlockchairAddressResponse> getAddress(@Path("blockchain") String blockchain, @Path("address") String address);

    @GET(Server.ApiBlockchair.Method.TRANSACTION)
    Single<BlockchairTransactionResponse> getTransaction(@Path("blockchain") String blockchain, @Path("transaction") String transaction);

    @GET(Server.ApiBlockchair.Method.STATS)
    Single<BlockchairStatsResponse> getStats(@Path("blockchain") String blockchain);

    @POST(Server.ApiBlockchair.Method.PUSH)
    Completable sendTransaction(@Path("blockchain") String blockchain, @Body BlockchairSendBody body);
}
