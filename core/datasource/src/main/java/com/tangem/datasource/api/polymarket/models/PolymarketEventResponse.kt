package com.tangem.datasource.api.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET /api/predictions/v1/events/{eventId}` (BFF `EventResponse`).
 */
@JsonClass(generateAdapter = true)
data class PolymarketEventResponse(
    @Json(name = "event") val event: PolymarketEventDto,
)