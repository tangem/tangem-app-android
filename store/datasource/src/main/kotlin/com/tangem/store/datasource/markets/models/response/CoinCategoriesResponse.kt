package com.tangem.store.datasource.markets.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoinCategoriesResponse(
    @Json(name = "categories") val categories: List<Category>,
) {

    /**
     * Coin category
     *
     * @param id          category id
     * @param code        stable machine-readable key, e.g. `stocks`, `commodities`
     * @param displayName name to show when no localized name is available
     * @param tokensCount total number of coins in the category
     * @param isRestricted category is region-restricted for the current user
     * @param sectors     sub-groups of the category; empty when the category has none
     */
    @JsonClass(generateAdapter = true)
    data class Category(
        @Json(name = "id") val id: String,
        @Json(name = "code") val code: String,
        @Json(name = "displayName") val displayName: String,
        @Json(name = "displayOrder") val displayOrder: Int,
        @Json(name = "tokensCount") val tokensCount: Int,
        @Json(name = "restricted") val isRestricted: Boolean = false,
        @Json(name = "visible") val isVisible: Boolean,
        @Json(name = "sectors") val sectors: List<Sector> = emptyList(),
    ) {

        /**
         * Sector of a [Category]
         *
         * @param id          sector id, e.g. `technology`
         * @param displayName name to show when no localized name is available
         * @param tokensCount number of coins in the sector
         */
        @JsonClass(generateAdapter = true)
        data class Sector(
            @Json(name = "id") val id: String,
            @Json(name = "displayName") val displayName: String,
            @Json(name = "tokensCount") val tokensCount: Int,
        )
    }
}