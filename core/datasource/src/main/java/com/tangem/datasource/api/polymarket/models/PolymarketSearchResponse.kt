package com.tangem.datasource.api.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET /api/predictions/v1/search` (BFF `SearchPageResponse`).
 *
 * Unlike the feed, search pages with a 1-based [page] number instead of a keyset cursor.
 */
@JsonClass(generateAdapter = true)
data class PolymarketSearchResponse(
    @Json(name = "events") val events: List<PolymarketEventDto>,
    @Json(name = "page") val page: Int,
    @Json(name = "total") val total: Int,
    @Json(name = "hasNext") val hasNext: Boolean,
)