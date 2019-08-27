package com.tangem.data.network;

import com.tangem.data.network.model.RateInfoResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface CoinmarketApi {
    @Headers("X-CMC_PRO_API_KEY: f6622117-c043-47a0-8975-9d673ce484de")
    @GET(Server.ApiCoinmarket.Method.PRICE_CONVERSION)
    Observable<RateInfoResponse> getRateInfo(@Query("amount") int amount, @Query("symbol") String cryptoId);
}