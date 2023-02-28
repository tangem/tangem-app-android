package com.tangem.tap.network.exchangeServices.mercuryo

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path


interface MercuryoApi {

    @GET("{apiVersion}/lib/currencies")
    suspend fun currencies(
        @Path("apiVersion") apiVersion: String,
    ): MercuryoCurrenciesResponse
}

data class MercuryoCurrenciesResponse(
    val status: Int,
    val data: Data,
) {
    data class Data(
        val fiat: List<String>,
        val crypto: List<String>,
        val config: Config,
    )

    data class Config(
        val base: Map<String, String>,
        @Json(name = "has_withdrawal_fee")
        val hasWithdrawalFee: Map<String, Boolean>,
        @Json(name = "display_options")
        val displayOptions: Map<String, DisplayOption>,
        val icons: Map<String, Any>,
    )

    data class DisplayOption(
        @Json(name = "fullname")
        val fullName: String,
        @Json(name = "total_digits")
        val totalDigits: Int,
        @Json(name = "display_digits")
        val displayDigits: Int,
    )
}