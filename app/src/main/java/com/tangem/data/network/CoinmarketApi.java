package com.tangem.data.network;

import com.tangem.data.network.model.RateInfoResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface CoinmarketApi {
    @Headers("X-CMC_PRO_API_KEY: 7850b957-6943-42eb-af73-8d536dd8f73e")
    @GET(Server.ApiCoinmarket.Method.PRICE_CONVERSION)
    Call<RateInfoResponse> getRateInfo(@Query("amount") int amount, @Query("symbol") String cryptoId);
}