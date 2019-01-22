package com.tangem.data.network;

import com.tangem.data.network.model.InfuraBody;
import com.tangem.data.network.model.InfuraResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RootstockApi {
    @Headers("Content-Type: application/json")
    @POST(Server.ApiRootstock.Method.MAIN)
    Call<InfuraResponse> rootstock(@Body InfuraBody body);
}
