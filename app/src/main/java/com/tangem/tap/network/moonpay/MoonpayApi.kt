package com.tangem.tap.network.moonpay

import retrofit2.http.GET
import retrofit2.http.Query

interface MoonpayApi {
    @GET(MOOONPAY_IP_ADDRESS_REQUEST_URL)
    suspend fun getUserStatus(
        @Query("apiKey") moonpayApiKey: String,
    ): MoonPayUserStatus

    @GET(MOOONPAY_CURRENCIES_REQUEST_URL)
    suspend fun getCurrencies(
        @Query("apiKey") moonpayApiKey: String,
    ): List<MoonpayCurrencies>


    companion object {
        const val MOOONPAY_BASE_URL = "https://api.moonpay.com/"
        const val MOOONPAY_IP_ADDRESS_REQUEST_URL = "v4/ip_address/"
        const val MOOONPAY_CURRENCIES_REQUEST_URL = "v3/currencies/"
    }
}