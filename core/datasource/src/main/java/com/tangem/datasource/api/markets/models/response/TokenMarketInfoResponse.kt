package com.tangem.datasource.api.markets.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class TokenMarketInfoResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "current_price")
    val currentPrice: BigDecimal,
    @Json(name = "price_change_percentage")
    val priceChangePercentage: PriceChangePercentage?,
    @Json(name = "networks")
    val networks: List<Network>?,
    @Json(name = "short_description")
    val shortDescription: String?,
    @Json(name = "full_description")
    val fullDescription: String?,
    @Json(name = "insights")
    val insights: Insights?,
    @Json(name = "metrics")
    val metrics: Metrics?,
    @Json(name = "security_data")
    val securityData: SecurityData?,
    @Json(name = "links")
    val links: Links?,
    @Json(name = "price_performance")
    val pricePerformance: PricePerformance?,
    @Json(name = "exchanges_amount")
    val exchangesAmount: Int?,
) {

    @JsonClass(generateAdapter = true)
    data class PriceChangePercentage(
        @Json(name = "24h")
        val day: BigDecimal?,
        @Json(name = "1w")
        val week: BigDecimal?,
        @Json(name = "1m")
        val month: BigDecimal?,
        @Json(name = "3m")
        val threeMonths: BigDecimal?,
        @Json(name = "6m")
        val sixMonths: BigDecimal?,
        @Json(name = "1y")
        val year: BigDecimal?,
        @Json(name = "all_time")
        val allTime: BigDecimal?,
    )

    @JsonClass(generateAdapter = true)
    data class Network(
        @Json(name = "network_id")
        val networkId: String,
        @Json(name = "exchangeable")
        val exchangeable: Boolean = false,
        @Json(name = "contract_address")
        val contractAddress: String?,
        @Json(name = "decimal_count")
        val decimalCount: Int?,
    )

    @JsonClass(generateAdapter = true)
    data class Insights(
        @Json(name = "holders_change")
        val holdersChange: Change?,
        @Json(name = "liquidity_change")
        val liquidityChange: Change?,
        @Json(name = "buy_pressure_change")
        val buyPressureChange: Change?,
        @Json(name = "experienced_buyer_change")
        val experiencedBuyerChange: Change?,
        @Json(name = "networks")
        val sourceNetworks: List<SourceNetwork>?,
    ) {
        @JsonClass(generateAdapter = true)
        data class SourceNetwork(
            @Json(name = "network_id")
            val id: String,
            @Json(name = "network_name")
            val name: String,
        )
    }

    @JsonClass(generateAdapter = true)
    data class Change(
        @Json(name = "24h")
        val day: BigDecimal?,
        @Json(name = "1w")
        val week: BigDecimal?,
        @Json(name = "1m")
        val month: BigDecimal?,
    )

    @JsonClass(generateAdapter = true)
    data class Metrics(
        @Json(name = "market_rating")
        val marketRating: Int?,
        @Json(name = "circulating_supply")
        val circulatingSupply: BigDecimal?,
        @Json(name = "market_cap")
        val marketCap: BigDecimal?,
        @Json(name = "volume_24h")
        val volume24h: BigDecimal?,
        @Json(name = "max_supply")
        val maxSupply: BigDecimal?,
        @Json(name = "fully_diluted_valuation")
        val fullyDilutedValuation: BigDecimal?,
    )

    @JsonClass(generateAdapter = true)
    data class Links(
        @Json(name = "official_links")
        val officialLinks: List<Link>? = null,
        @Json(name = "social")
        val social: List<Link>? = null,
        @Json(name = "repository")
        val repository: List<Link>? = null,
        @Json(name = "blockchain_site")
        val blockchainSite: List<Link>? = null,
    )

    @JsonClass(generateAdapter = true)
    data class Link(
        @Json(name = "title")
        val title: String,
        @Json(name = "id")
        val id: String?,
        @Json(name = "link")
        val link: String,
    )

    @JsonClass(generateAdapter = true)
    data class PricePerformance(
        @Json(name = "24h")
        val day: Range?,
        @Json(name = "1m")
        val month: Range?,
        @Json(name = "all_time")
        val allTime: Range?,
    )

    @JsonClass(generateAdapter = true)
    data class SecurityData(
        @Json(name = "total_security_score")
        val totalSecurityScore: Float,
        @Json(name = "provider_data")
        val providerData: List<ProviderData>,
    )

    @JsonClass(generateAdapter = true)
    data class ProviderData(
        @Json(name = "provider_id")
        val providerId: String,
        @Json(name = "provider_name")
        val providerName: String,
        @Json(name = "link")
        val link: String?,
        @Json(name = "security_score")
        val securityScore: Float,
        @Json(name = "last_audit_date")
        val lastAuditDate: DateTime?,
    )

    @JsonClass(generateAdapter = true)
    data class Range(
        @Json(name = "low_price")
        val low: BigDecimal?,
        @Json(name = "high_price")
        val high: BigDecimal?,
    )
}