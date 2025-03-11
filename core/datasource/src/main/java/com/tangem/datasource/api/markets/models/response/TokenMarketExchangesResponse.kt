package com.tangem.datasource.api.markets.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Token market exchanges response
 *
 * @property exchanges list of exchanges
 *
[REDACTED_AUTHOR]
 */
@JsonClass(generateAdapter = true)
data class TokenMarketExchangesResponse(
    @Json(name = "exchanges") val exchanges: List<Exchange>,
) {

    /**
     * Exchange
     *
     * @property id            id
     * @property name          name
     * @property imageUrl      image url
     * @property isCentralized CEX (true), DEX (false)
     * @property volumeInUsd   aggregated volume in USD
     * @property trustScore    trust score
     */
    @JsonClass(generateAdapter = true)
    data class Exchange(
        @Json(name = "exchange_id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "image") val imageUrl: String?,
        @Json(name = "centralized") val isCentralized: Boolean,
        @Json(name = "volume_usd") val volumeInUsd: BigDecimal,
        @Json(name = "trust_score") val trustScore: Int?,
    )
}