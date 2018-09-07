package com.tangem.data.network;

import retrofit2.Call;
import retrofit2.http.GET;

public interface EstimatefeeApi {
    @GET(Server.ApiEstimatefee.Method.N_2)
    Call<String> getEstimateFeePriority();

    @GET(Server.ApiEstimatefee.Method.N_3)
    Call<String> getEstimateFeeNormal();

    @GET(Server.ApiEstimatefee.Method.N_6)
    Call<String> getEstimateFeeMinimal();
}