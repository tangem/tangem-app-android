package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * A prediction event — the primary Discovery feed entity (BFF `Event`).
 *
 * @property id unique event id
 * @property slug url-friendly event identifier
 * @property title event title
 * @property description event description, also used as the resolution criteria text
 * @property rulesUrl link to the Polymarket rules of the event
 * @property iconUrl optional event icon
 * @property imageUrl optional event image
 * @property status lifecycle status of the event
 * @property startDate raw upstream start date, if known. The format is not confirmed yet, so it is kept as reported
 * @property endDate raw upstream end date, if known. The format is not confirmed yet, so it is kept as reported
 * @property volume total traded volume in USD, if known
 * @property volume24h traded volume in USD over the last 24 hours, if known
 * @property liquidity available liquidity in USD, if known
 * @property totalMarketsCount total number of markets in the event (may exceed [markets] size,
 *  since the Discovery feed carries only the top active markets)
 * @property isNegRisk whether the event's markets are mutually exclusive
 * @property displayMode how the event and its [markets] are rendered
 * @property markets nested markets (top active ones for the feed, all for event details)
 */
data class PolymarketEvent(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val rulesUrl: String,
    val iconUrl: String?,
    val imageUrl: String?,
    val status: PolymarketStatus,
    val startDate: String?,
    val endDate: String?,
    val volume: BigDecimal?,
    val volume24h: BigDecimal?,
    val liquidity: BigDecimal?,
    val totalMarketsCount: Int,
    val isNegRisk: Boolean,
    val displayMode: PolymarketDisplayMode,
    val markets: List<PolymarketMarket>,
)