package com.tangem.data.network;

import com.tangem.data.network.model.BlockchainInfoAddress;
import com.tangem.data.network.model.BlockchainInfoUnspents;

import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BlockchainInfoApi {
    @GET(Server.ApiBlockchainInfo.Method.ADDRESS)
    Single<BlockchainInfoAddress> blockchainInfoAddress(@Path("address") String address, @Query("offset") Integer offset);

    @GET(Server.ApiBlockchainInfo.Method.UTXO)
    Single<BlockchainInfoUnspents> blockchainInfoUnspents(@Query("active") String address);

    @FormUrlEncoded
    @POST(Server.ApiBlockchainInfo.Method.PUSH)
    Single<ResponseBody> blockchainInfoPush(@Field("tx") String tx);
}
