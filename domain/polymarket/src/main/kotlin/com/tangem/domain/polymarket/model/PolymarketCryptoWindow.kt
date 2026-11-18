package com.tangem.domain.polymarket.model

import java.math.BigDecimal
import java.time.Instant

/**
 * The time window of a crypto Up/Down event: the event resolves by where the coin's price ends relative to
 * [priceToBeat].
 *
 * @property start when the window opens
 * @property end when the window closes
 * @property priceFeed source of the coin's live price, absent when the backend names none
 * @property priceToBeat the coin's price at [start], absent until the backend has fixed it
 * @property finalPrice the coin's price at [end], absent until the backend has confirmed it
 * @property nextWindows the windows that follow, nearest first; sent with event details only, empty in the feed
 * @property liveEventId event whose window is live right now; sent with event details only
 */
data class PolymarketCryptoWindow(
    val start: Instant,
    val end: Instant,
    val priceFeed: PolymarketCryptoPriceFeed?,
    val priceToBeat: BigDecimal?,
    val finalPrice: BigDecimal?,
    val nextWindows: List<PolymarketCryptoNextWindow>,
    val liveEventId: String?,
)

/** A later window of the same series; each window is an event of its own. */
data class PolymarketCryptoNextWindow(
    val eventId: String,
    val start: Instant,
    val end: Instant,
)

/**
 * Where the coin's live price is streamed from; also identifies the subscription.
 *
 * @property channel price channel of the live-data socket
 * @property symbol the coin's pair on that channel, e.g. `btcusd`
 * @property provider price provider the subscription is pinned to, when the backend names one
 * @property windowSeconds averaging window of a TWAP channel, absent for a spot channel
 */
data class PolymarketCryptoPriceFeed(
    val channel: String,
    val symbol: String,
    val provider: String?,
    val windowSeconds: Int?,
)