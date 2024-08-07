package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenMarketInfo(
    val id: String,
    val name: String,
    val symbol: String,
    val currentPrice: BigDecimal,
    val priceChangePercentage: PriceChangePercentage?,
    val networks: List<Network>?,
    val shortDescription: String?,
    val fullDescription: String?,
    val insights: List<Insight>?,
    val metrics: Metrics?,
    val links: Links?,
    val pricePerformance: PricePerformance?,
) {
    data class PriceChangePercentage(
        val day: Int?,
        val week: Int?,
        val month: Int?,
        val threeMonths: Int?,
        val sixMonths: Int?,
        val year: Int?,
        val allTime: Int?,
    )

    data class Network(
        val networkId: String,
        val exchangeable: Boolean,
        val contractAddress: String,
        val decimalCount: Int,
    )

    data class Insight(
        val holdersChange: Change?,
        val liquidityChange: Change?,
        val buyPressureChange: Change?,
        val experiencedBuyerChange: Change?,
    )

    data class Change(
        val day: Int?,
        val week: Int?,
        val month: Int?,
    )

    data class Metrics(
        val marketRating: Int?,
        val circulatingSupply: BigDecimal?,
        val marketCap: BigDecimal?,
        val volume24h: BigDecimal?,
        val totalSupply: BigDecimal?,
        val fullyDilutedValuation: BigDecimal?,
    )

    data class Links(
        val officialLinks: List<Link>?,
        val social: List<Link>?,
        val repository: List<Link>?,
        val blockchainSite: List<Link>?,
    )

    data class Link(
        val title: String,
        val id: String?,
        val link: String,
    )

    data class PricePerformance(
        val day: Range?,
        val month: Range?,
        val allTime: Range?,
    )

    data class Range(
        val low: Int?,
        val high: Int?,
    )
}
