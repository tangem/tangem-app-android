package com.tangem.datasource.api.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET /api/predictions/v1/categories` (BFF `CategoriesResponse`).
 */
@JsonClass(generateAdapter = true)
data class PolymarketCategoriesResponse(
    @Json(name = "categories") val categories: List<PolymarketCategoryDto>,
)

/** BFF `Category`. */
@JsonClass(generateAdapter = true)
data class PolymarketCategoryDto(
    @Json(name = "id") val id: Int,
    @Json(name = "label") val label: String,
    @Json(name = "icon") val icon: String?,
)