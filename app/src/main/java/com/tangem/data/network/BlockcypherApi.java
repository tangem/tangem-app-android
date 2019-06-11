package com.tangem.data.network;

import com.tangem.data.network.model.BlockcypherBody;
import com.tangem.data.network.model.BlockcypherResponse;
import com.tangem.data.network.model.BlockcypherFee;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BlockcypherApi {
    @GET(Server.ApiBlockcypher.Method.MAIN)
    Call<BlockcypherFee> blockcypherMain(@Path("blockchain") String blockchain);

    @GET(Server.ApiBlockcypher.Method.ADDRESS)
    Call<BlockcypherResponse> blockcypherAddress(@Path("blockchain") String blockchain, @Path("address") String address);

    @Headers("Content-Type: application/json")
    @POST(Server.ApiBlockcypher.Method.PUSH)
    Call<BlockcypherResponse> blockcypherPush(@Path("blockchain") String blockchain, @Body BlockcypherBody blockcypherBody, @Query("token") String token);
}
