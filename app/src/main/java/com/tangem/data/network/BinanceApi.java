package com.tangem.data.network;

import com.tangem.data.network.model.BinanceFees;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface BinanceApi {
    @GET("fees")
    Call<List<BinanceFees>> binanceFees();
}
