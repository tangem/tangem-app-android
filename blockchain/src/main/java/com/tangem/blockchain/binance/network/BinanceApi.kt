package com.tangem.blockchain.binance.network

import retrofit2.Call
import retrofit2.http.GET

interface BinanceApi {
    @GET("api/v1/fees")
    suspend fun getFees(): List<BinanceFee>
}