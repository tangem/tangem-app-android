package com.tangem.tap.network.exchangeServices.moonpay

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface MoonPayApi {

    @GET(MOOONPAY_IP_ADDRESS_REQUEST_URL)
    suspend fun getUserStatus(@Query("apiKey") moonPayApiKey: String): MoonPayUserStatus

    @GET(MOOONPAY_CURRENCIES_REQUEST_URL)
    suspend fun getCurrencies(@Query("apiKey") moonPayApiKey: String): List<MoonPayCurrencies>

    companion object {
        const val MOOONPAY_BASE_URL = "https://api.moonpay.com/"
        const val MOOONPAY_IP_ADDRESS_REQUEST_URL = "v4/ip_address/"
        const val MOOONPAY_CURRENCIES_REQUEST_URL = "v3/currencies/"
    }
}

@JsonClass(generateAdapter = true)
data class MoonPayUserStatus(
    @Json(name = "isBuyAllowed")
    val isBuyAllowed: Boolean,
    @Json(name = "isSellAllowed")
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
    @Json(name = "type") val type: String,
    @Json(name = "code") val code: String,
    @Json(name = "supportsLiveMode") val supportsLiveMode: Boolean = false,
    @Json(name = "isSuspended") val isSuspended: Boolean = true,
    @Json(name = "isSupportedInUS") val isSupportedInUS: Boolean = false,
    @Json(name = "isSellSupported") val isSellSupported: Boolean = false,
    @Json(name = "notAllowedUSStates") val notAllowedUSStates: List<String> = emptyList(),
    @Json(name = "metadata") val metadata: MoonPayCurrenciesMetadata? = null,
)

@JsonClass(generateAdapter = true)
data class MoonPayCurrenciesMetadata(
    @Json(name = "contractAddress") val contractAddress: String?,
    @Json(name = "networkCode") val networkCode: String?,
)