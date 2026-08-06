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

/**
 * BFF `Event`.
 *
 * [status] and [displayMode] are kept as raw strings so that a value the app does not know about yet fails to
 * convert instead of failing to parse the whole feed.
 */
@JsonClass(generateAdapter = true)
data class PolymarketEventDto(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "polymarketRulesUrl") val polymarketRulesUrl: String,
    @Json(name = "icon") val icon: String?,
    @Json(name = "image") val image: String?,
    @Json(name = "status") val status: String,
    @Json(name = "startDate") val startDate: String?,
    @Json(name = "endDate") val endDate: String?,
    @Json(name = "volume") val volume: Double?,
    @Json(name = "volume24hr") val volume24hr: Double?,
    @Json(name = "liquidity") val liquidity: Double?,
    @Json(name = "totalMarketsCount") val totalMarketsCount: Int,
    @Json(name = "negRisk") val isNegRisk: Boolean,
    @Json(name = "displayMode") val displayMode: String,
    @Json(name = "markets") val markets: List<PolymarketMarketDto>,
)

/** BFF `Market`. The feed carries only the top active markets of an event, event details carry all of them. */
@JsonClass(generateAdapter = true)
data class PolymarketMarketDto(
    @Json(name = "id") val id: String,
    @Json(name = "eventId") val eventId: String,
    @Json(name = "question") val question: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "description") val description: String,
    @Json(name = "groupItemTitle") val groupItemTitle: String?,
    @Json(name = "icon") val icon: String?,
    @Json(name = "image") val image: String?,
    @Json(name = "status") val status: String,
    @Json(name = "negRisk") val isNegRisk: Boolean,
    @Json(name = "startDate") val startDate: String?,
    @Json(name = "endDate") val endDate: String?,
    @Json(name = "startDateIso") val startDateIso: String?,
    @Json(name = "endDateIso") val endDateIso: String?,
    @Json(name = "volume") val volume: Double?,
    @Json(name = "volume24hr") val volume24hr: Double?,
    @Json(name = "liquidity") val liquidity: Double?,
    @Json(name = "orderIndex") val orderIndex: Int,
    @Json(name = "outcomes") val outcomes: List<PolymarketOutcomeDto>,
)

/** BFF `OutcomeSummary`. */
@JsonClass(generateAdapter = true)
data class PolymarketOutcomeDto(
    @Json(name = "assetId") val assetId: String,
    @Json(name = "label") val label: String,
    @Json(name = "probability") val probability: Double?,
)