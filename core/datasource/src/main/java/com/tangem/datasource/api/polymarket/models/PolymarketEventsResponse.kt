package com.tangem.datasource.api.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET /api/predictions/v1/events` (BFF `EventsPageResponse`).
 */
@JsonClass(generateAdapter = true)
data class PolymarketEventsResponse(
    @Json(name = "events") val events: List<PolymarketEventDto>,
    @Json(name = "cursor") val cursor: String?,
    @Json(name = "hasNext") val hasNext: Boolean,
)

/** BFF `Event`. Only the fields consumed by the app are declared; unknown JSON is ignored. */
@JsonClass(generateAdapter = true)
data class PolymarketEventDto(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "title") val title: String,
    @Json(name = "icon") val icon: String?,
    @Json(name = "image") val image: String?,
    @Json(name = "volume") val volume: Double?,
    @Json(name = "totalMarketsCount") val totalMarketsCount: Int,
    @Json(name = "markets") val markets: List<PolymarketMarketDto>,
)

/** BFF `Market` (reduced preview inside an [PolymarketEventDto]). */
@JsonClass(generateAdapter = true)
data class PolymarketMarketDto(
    @Json(name = "id") val id: String,
    @Json(name = "question") val question: String,
    @Json(name = "icon") val icon: String?,
    @Json(name = "volume") val volume: Double?,
    @Json(name = "outcomes") val outcomes: List<PolymarketOutcomeDto>,
)

/** BFF `OutcomeSummary`. */
@JsonClass(generateAdapter = true)
data class PolymarketOutcomeDto(
    @Json(name = "assetId") val assetId: String,
    @Json(name = "label") val label: String,
    @Json(name = "probability") val probability: Double?,
)