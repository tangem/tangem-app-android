package com.tangem.datasource.api.markets.models.response

import com.squareup.moshi.Json
import java.math.BigDecimal

data class TokenMarketDetailsResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "active")
    val active: Boolean,
    @Json(name = "current_price")
    val currentPrice: BigDecimal,
    @Json(name = "price_change_percentage")
    val priceChangePercentage: PriceChangePercentage,
    @Json(name = "networks")
    val networks: List<Network>,
    @Json(name = "short_description")
    val shortDescription: String?,
    @Json(name = "full_description")
    val fullDescription: String?,
    @Json(name = "insights")
    val insights: List<Insight>?,
    @Json(name = "metrics")
    val metrics: Metrics,
    @Json(name = "links")
    val links: Links,
    @Json(name = "price_performance")
    val pricePerformance: PricePerformance,
) {
    data class PriceChangePercentage(
        @Json(name = "24h")
        val h24: BigDecimal,
        @Json(name = "1w")
        val week1: BigDecimal,
        @Json(name = "1m")
        val month1: BigDecimal,
        @Json(name = "3m")
        val month3: BigDecimal,
        @Json(name = "6m")
        val month6: BigDecimal,
        @Json(name = "1y")
        val year1: BigDecimal,
        @Json(name = "all_time")
        val allTime: BigDecimal,
    )

    data class Network(
        @Json(name = "network_id")
        val networkId: String,
        @Json(name = "exchangeable")
        val exchangeable: Boolean,
        @Json(name = "contract_address")
        val contractAddress: String,
        @Json(name = "decimalCount")
        val decimalCount: Int,
    )

    data class Insight(
        @Json(name = "holders_change")
        val holdersChange: Change,
        @Json(name = "liquidity_change")
        val liquidityChange: Change,
        @Json(name = "buy_pressure_change")
        val buyPressureChange: Change,
        @Json(name = "experienced_buyer_change")
        val experiencedBuyerChange: Change,
    ) {
        data class Change(
            @Json(name = "1d")
            val day1: Int,
            @Json(name = "1w")
            val week1: Int,
            @Json(name = "1m")
            val month1: Int,
        )
    }

    data class Metrics(
        @Json(name = "market_rating")
        val marketRating: Int,
        @Json(name = "circulating_supply")
        val circulatingSupply: BigDecimal,
        @Json(name = "market_cap")
        val marketCap: BigDecimal,
        @Json(name = "volume_24h")
        val volume24h: BigDecimal,
        @Json(name = "total_supply")
        val totalSupply: BigDecimal,
        @Json(name = "fully_diluted_valuation")
        val fullyDilutedValuation: BigDecimal,
    )

    data class Links(
        @Json(name = "official_links")
        val officialLinks: List<Link> = emptyList(),
        @Json(name = "social")
        val social: List<Link> = emptyList(),
        @Json(name = "repository")
        val repository: List<Link> = emptyList(),
        @Json(name = "blockchain_site")
        val blockchainSite: List<Link> = emptyList(),
    )

    data class Link(
        @Json(name = "title")
        val title: String?,
        @Json(name = "id")
        val id: String,
        @Json(name = "link")
        val url: String,
    )

    data class PricePerformance(
        @Json(name = "high_price")
        val highPrice: Price,
        @Json(name = "low_price")
        val lowPrice: Price,
    ) {
        data class Price(
            @Json(name = "24h")
            val h24: BigDecimal,
            @Json(name = "1m")
            val month1: BigDecimal,
            @Json(name = "all_time")
            val allTime: BigDecimal,
        )
    }
}
