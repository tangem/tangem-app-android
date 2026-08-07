package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * A prediction event — the primary Discovery feed entity (BFF `Event`).
 *
 * @property id unique event id
 * @property slug url-friendly event identifier
 * @property title event title
 * @property iconUrl optional event icon
 * @property imageUrl optional event image
 * @property volume total traded volume in USD, if known
 * @property totalMarketsCount total number of markets in the event (may exceed [markets] size,
 *  since the Discovery feed carries only the top active markets)
 * @property markets nested markets (top active ones for the feed, all for event details)
 */
data class PolymarketEvent(
    val id: String,
    val slug: String,
    val title: String,
    val iconUrl: String?,
    val imageUrl: String?,
    val volume: BigDecimal?,
    val totalMarketsCount: Int,
    val markets: List<PolymarketMarket>,
)