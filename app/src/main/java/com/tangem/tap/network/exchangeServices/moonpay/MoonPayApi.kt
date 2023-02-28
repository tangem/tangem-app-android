package com.tangem.tap.network.exchangeServices.moonpay

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface MoonPayApi {

    @GET(MOOONPAY_IP_ADDRESS_REQUEST_URL)
    suspend fun getUserStatus(
        @Query("apiKey") moonPayApiKey: String,
    ): MoonPayUserStatus

    @GET(MOOONPAY_CURRENCIES_REQUEST_URL)
    suspend fun getCurrencies(
        @Query("apiKey") moonPayApiKey: String,
    ): List<MoonPayCurrencies>

    companion object {
        const val MOOONPAY_BASE_URL = "https://api.moonpay.com/"
        const val MOOONPAY_IP_ADDRESS_REQUEST_URL = "v4/ip_address/"
        const val MOOONPAY_CURRENCIES_REQUEST_URL = "v3/currencies/"
    }
}

@JsonClass(generateAdapter = true)
data class MoonPayUserStatus(
    val isBuyAllowed: Boolean,
    val isSellAllowed: Boolean,
    @Json(name = "isAllowed")
    val isMoonpayAllowed: Boolean,
    @Json(name = "alpha3")
    val countryCode: String,
    @Json(name = "state")
    val stateCode: String,
)

@JsonClass(generateAdapter = true)
data class MoonPayCurrencies(
    val type: String,
    val code: String,
    val supportsLiveMode: Boolean = false,
    val isSuspended: Boolean = true,
    val isSupportedInUS: Boolean = false,
    val isSellSupported: Boolean = false,
    val notAllowedUSStates: List<String> = emptyList(),
)