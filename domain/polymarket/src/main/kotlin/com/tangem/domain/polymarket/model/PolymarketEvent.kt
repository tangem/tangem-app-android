package com.tangem.domain.polymarket.model

import java.math.BigDecimal
import java.time.Instant

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
 * @property gameId upstream game identifier, absent for non-sports events
 * @property score raw provider score, which may include several sets
 * @property period raw provider period or game status, e.g. `S2` or `FT`
 * @property isEnded whether the game has finished; absent when not reported
 * @property dataFetchedAt when the backend fetched the event, used to determine sports data freshness
 * @property teams teams or players in the order supplied by the provider
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
    val isLive: Boolean,
    val displayMode: PolymarketDisplayMode,
    val markets: List<PolymarketMarket>,
    val gameId: String? = null,
    val score: String? = null,
    val period: String? = null,
    val isEnded: Boolean? = null,
    val dataFetchedAt: Instant? = null,
    val teams: List<PolymarketTeam>? = null,
)