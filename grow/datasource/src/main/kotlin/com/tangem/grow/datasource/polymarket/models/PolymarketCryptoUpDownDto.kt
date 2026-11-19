package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * BFF `cryptoUpDown`: the window of a crypto Up/Down event. Only those events carry it — `eventType` is `crypto`
 * for every crypto event and does not tell them apart.
 *
 * The window ends at the event's `endDate`. [nextEvents] and [liveEventId] come with event details only; the
 * feed sends `null` for both.
 */
@JsonClass(generateAdapter = true)
data class PolymarketCryptoUpDownDto(
    @Json(name = "windowStart") val windowStart: String? = null,
    @Json(name = "priceFeed") val priceFeed: PolymarketCryptoPriceFeedDto? = null,
    @Json(name = "priceToBeat") val priceToBeat: Double? = null,
    @Json(name = "finalPrice") val finalPrice: Double? = null,
    @Json(name = "nextEvents") val nextEvents: List<PolymarketCryptoNextEventDto>? = null,
    @Json(name = "liveEventId") val liveEventId: String? = null,
)

/** Where the coin's live price is streamed from: a PolyBolt [channel] and its subscription [filter]. */
@JsonClass(generateAdapter = true)
data class PolymarketCryptoPriceFeedDto(
    @Json(name = "channel") val channel: String? = null,
    @Json(name = "filter") val filter: PolymarketCryptoPriceFeedFilterDto? = null,
)

/** The TWAP channel names [windowSeconds], the spot channel names [provider]. */
@JsonClass(generateAdapter = true)
data class PolymarketCryptoPriceFeedFilterDto(
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "provider") val provider: String? = null,
    @Json(name = "window_seconds") val windowSeconds: Int? = null,
)

@JsonClass(generateAdapter = true)
data class PolymarketCryptoNextEventDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "windowStart") val windowStart: String? = null,
    @Json(name = "windowEnd") val windowEnd: String? = null,
)