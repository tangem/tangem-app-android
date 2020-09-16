package com.tangem.tap.network.coinmarketcap

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
class RateInfoResponse : CoinMarketResponse<RateData>()

@JsonClass(generateAdapter = true)
data class RateData(
        val quote: Map<String, CurrencyRate>
) {
    fun getRate(): BigDecimal = quote.values.first().price
}

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

@JsonClass(generateAdapter = true)
class FiatMapResponse : CoinMarketResponse<List<FiatCurrency>>()

@JsonClass(generateAdapter = true)
open class CoinMarketResponse<T : Any> {
    lateinit var status: Status
    lateinit var data: T
}

@JsonClass(generateAdapter = true)
data class FiatCurrency(
        val id: Int,
        val name: String,
        val sign: String,
        val symbol: String
)
