package com.tangem.datasource.api.markets.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class TokenMarketListResponse(
    @Json(name = "imageHost") val imageHost: String?,
    @Json(name = "tokens") val tokens: List<Token>,
    @Json(name = "total") val total: Int,
    @Json(name = "limit") val limit: Int,
    @Json(name = "offset") val offset: Int,
    @Json(name = "timestamp") val timestamp: Long? = null,
    @Json(name = "summary") val summary: Summary? = null,
) {

    @JsonClass(generateAdapter = true)
    data class Token(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "current_price") val currentPrice: BigDecimal,
        @Json(name = "price_change_percentage") val priceChangePercentage: PriceChangePercentage?,
        @Json(name = "market_rating") val marketRating: Int?,
        @Json(name = "market_cap") val marketCap: BigDecimal?,
        @Json(name = "is_under_market_cap_limit") val isUnderMarketCapLimit: Boolean?,
        @Json(name = "staking_opportunities") val stakingOpportunities: List<StakingOpportunities>?,
    ) {

        @JsonClass(generateAdapter = true)
        data class PriceChangePercentage(
            @Json(name = "24h") val h24: BigDecimal?,
            @Json(name = "1w") val week1: BigDecimal?,
            @Json(name = "30d") val day30: BigDecimal?,
        )

        @JsonClass(generateAdapter = true)
        data class StakingOpportunities(
            @Json(name = "id") val id: Int?,
            @Json(name = "apy") val apy: BigDecimal?,
            @Json(name = "network_id") val networkId: String?,
            @Json(name = "reward_type") val rewardType: RewardType?,
        )

        @JsonClass(generateAdapter = false)
        enum class RewardType {
            @Json(name = "apy") APY,

            @Json(name = "apr") APR,

            UNKNOWN,
        }
    }

    @JsonClass(generateAdapter = true)
    data class Summary(
        @Json(name = "max_apy") val maxApy: BigDecimal?,
    )
}