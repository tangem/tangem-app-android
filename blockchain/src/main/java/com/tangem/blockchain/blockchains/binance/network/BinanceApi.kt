package com.tangem.blockchain.blockchains.binance.network

import retrofit2.http.GET

interface BinanceApi {
    @GET("api/v1/fees")
    suspend fun getFees(): List<BinanceFee>
}