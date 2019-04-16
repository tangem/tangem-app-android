package com.tangem.data.network;

import com.tangem.data.network.model.BinanceAccount;
import com.tangem.data.network.model.BinanceError;
import com.tangem.data.network.model.BinanceFees;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface BinanceApi {
    @GET(Server.ApiBinance.Method.ACCOUNT)
    Call<BinanceAccount> binanceAccount();

    @GET(Server.ApiBinance.Method.FEES)
    Call<BinanceFees> binanceFees();

    @Headers("Content-Type: text/plain")
    @POST(Server.ApiBinance.Method.BROADCAST)
    Call<BinanceError> binanceBroadcast(@Body String txForSend);
}
