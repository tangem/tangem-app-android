package com.tangem.tap.network.moonpay

import retrofit2.http.GET
import retrofit2.http.Query

interface MoonpayApi {
    @GET( MOOONPAY_IP_ADDRESS_REQUEST_URL)
    suspend fun getUserStatus(
        @Query("apiKey") moonpayApiKey: String
    ): MoonPayUserStatus

    companion object {
        const val MOOONPAY_BASE_URL = "https://api.moonpay.com/v4/"
        const val MOOONPAY_IP_ADDRESS_REQUEST_URL = "ip_address/"
    }
}