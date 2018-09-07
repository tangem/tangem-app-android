package com.tangem.data.network;

import com.tangem.data.network.model.InfuraBody;
import com.tangem.data.network.model.InfuraResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface InfuraApi {
    @Headers("Content-Type: application/json")
    @POST(Server.ApiInfura.Method.MAIN)
    Call<InfuraResponse> infura(@Body InfuraBody body);
}