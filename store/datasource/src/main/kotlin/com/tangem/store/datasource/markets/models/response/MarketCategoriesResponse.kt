package com.tangem.store.datasource.markets.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class MarketCategoriesResponse(
    @Json(name = "categories") val categories: List<Category>,
    @Json(name = "total") val total: Int,
    @Json(name = "offset") val offset: Int,
    @Json(name = "limit") val limit: Int,
) {

    /**
     * Market category with its aggregated market data
     *
     * @param description        may be `null` or blank — most categories outside the top ~500 have no text
     * @param marketCap          `null` for categories the provider has no market data for
     * @param marketCapChange24h market cap change over the last 24 hours **in percent**, e.g. `-1.52`
     * @param isFeatured         category is curated as featured by the backend
     * @param tokensCount        number of coins in the category
     */
    @JsonClass(generateAdapter = true)
    data class Category(
        @Json(name = "id") val id: String,
        @Json(name = "name") val name: String,
        @Json(name = "description") val description: String? = null,
        @Json(name = "marketCap") val marketCap: BigDecimal? = null,
        @Json(name = "marketCapChange24h") val marketCapChange24h: BigDecimal? = null,
        @Json(name = "volume24h") val volume24h: BigDecimal? = null,
        @Json(name = "featured") val isFeatured: Boolean = false,
        @Json(name = "tokensCount") val tokensCount: Int = 0,
    )
}