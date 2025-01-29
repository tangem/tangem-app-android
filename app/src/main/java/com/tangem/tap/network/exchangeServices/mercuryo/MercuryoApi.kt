package com.tangem.tap.network.exchangeServices.mercuryo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path


interface MercuryoApi {

    @GET("{apiVersion}/lib/currencies")
    suspend fun currencies(@Path("apiVersion") apiVersion: String): MercuryoCurrenciesResponse
}

@JsonClass(generateAdapter = true)
data class MercuryoCurrenciesResponse(
    @Json(name = "status") val status: Int,
    @Json(name = "data") val data: Data,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "fiat") val fiat: List<String>,
        @Json(name = "crypto") val crypto: List<String>,
        @Json(name = "config") val config: Config,
    )

    @JsonClass(generateAdapter = true)
    data class Config(
        @Json(name = "crypto_currencies")
        val cryptoCurrencies: List<MercuryoCryptoCurrency>,
    )

    @JsonClass(generateAdapter = true)
    data class MercuryoCryptoCurrency(
        @Json(name = "currency")
        val currencySymbol: String,
        @Json(name = "network")
        val network: String,
        @Json(name = "contract")
        val contractAddress: String,
    )
}