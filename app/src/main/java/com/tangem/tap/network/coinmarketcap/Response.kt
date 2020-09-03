package com.tangem.tap.network.coinmarketcap

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class RateInfoResponse(
        val status: Status,
        val data: RateData
)

@JsonClass(generateAdapter = true)
data class RateData(
        val quote: Quote
)

@JsonClass(generateAdapter = true)
data class Quote(
        @Json(name = "USD")
        val usd: CurrencyRate
)

@JsonClass(generateAdapter = true)
data class CurrencyRate(
        val price: BigDecimal
)


@JsonClass(generateAdapter = true)
data class Status(
        val timestamp: String,
        @Json(name = "error_code")
        val errorCode: Int,
        @Json(name = "error_message")
        val errorMessage: String?,
        val elapsed: Int,
        @Json(name = "credit_count")
        val creditCount: Int,
        val notice: String?
)