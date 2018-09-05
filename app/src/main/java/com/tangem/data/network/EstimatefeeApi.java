package com.tangem.data.network;

import retrofit2.Call;
import retrofit2.http.GET;

public interface EstimatefeeApi {
    @GET(Server.ApiEstimatefee.Method.N)
    Call<String> getEstimatefee();
}