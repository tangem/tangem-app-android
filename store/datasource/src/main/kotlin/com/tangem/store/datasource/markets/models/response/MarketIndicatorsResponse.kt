package com.tangem.store.datasource.markets.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Global market indicators of the "Total market cap" block.
 *
 * Every field is nullable: the test environment answers with all of them as `null`, so a missing
 * indicator is a normal response rather than an error.
 *
 * @param totalMarketCap               total capitalisation
 * @param marketCapChangePercentage24h 24h capitalisation change in **percent** (`-1.61` = −1.61 %)
 * @param fearGreedValue               Fear & Greed index position on the `0..100` scale
 * @param fearGreedClassification      Fear & Greed label, localised by the backend
 * @param altcoinIndexValue            Altcoin Season index position on the `0..100` scale
 * @param altcoinIndexClassification   Altcoin Season label, localised by the backend
 */
@JsonClass(generateAdapter = true)
data class MarketIndicatorsResponse(
    @Json(name = "total_market_cap") val totalMarketCap: BigDecimal? = null,
    @Json(name = "market_cap_change_percentage_24h") val marketCapChangePercentage24h: BigDecimal? = null,
    @Json(name = "fear_greed_value") val fearGreedValue: Int? = null,
    @Json(name = "fear_greed_classification") val fearGreedClassification: String? = null,
    @Json(name = "altcoin_index_value") val altcoinIndexValue: Int? = null,
    @Json(name = "altcoin_index_classification") val altcoinIndexClassification: String? = null,
)