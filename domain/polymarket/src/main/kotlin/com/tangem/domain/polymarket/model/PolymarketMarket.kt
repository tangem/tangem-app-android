package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * A single Polymarket prediction market nested inside a [PolymarketEvent] (BFF `Market`).
 *
 * @property id unique market id
 * @property conditionId on-chain condition the market settles on; with an outcome's asset id it names the
 *  position a filled order creates, so it is known before the position exists
 * @property eventId id of the [PolymarketEvent] the market belongs to
 * @property title the market question (e.g. "Will X happen by 2026?")
 * @property slug url-friendly market identifier
 * @property description market description, also used as the resolution criteria text
 * @property groupItemTitle short market label used when the parent event is rendered as
 *  [PolymarketDisplayMode.GROUPED_OUTCOMES] (e.g. "France" for "Will France win the 2026 World Cup?")
 * @property iconUrl optional market icon
 * @property imageUrl optional market image
 * @property status lifecycle status of the market
 * @property isNegRisk whether the market belongs to a mutually exclusive group
 * @property isAcceptingOrders whether the book takes an order right now; a market can be [status] active and
 *  still refuse one
 * @property startDate raw upstream start date, if known. The format is not confirmed yet, so it is kept as reported
 * @property endDate raw upstream end date, if known. The format is not confirmed yet, so it is kept as reported
 * @property startDateIso upstream start date in a separate representation, if known
 * @property endDateIso upstream end date in a separate representation, if known
 * @property volume total traded volume in USD, if known
 * @property volume24h traded volume in USD over the last 24 hours, if known
 * @property liquidity available liquidity in USD, if known
 * @property orderIndex position of the market within its event
 * @property outcomes normalized outcomes with their implied probabilities
 */
data class PolymarketMarket(
    val id: String,
    val conditionId: String?,
    val eventId: String,
    val title: String,
    val slug: String,
    val description: String,
    val groupItemTitle: String?,
    val iconUrl: String?,
    val imageUrl: String?,
    val status: PolymarketStatus,
    val isNegRisk: Boolean,
    val isAcceptingOrders: Boolean,
    val startDate: String?,
    val endDate: String?,
    val startDateIso: String?,
    val endDateIso: String?,
    val volume: BigDecimal?,
    val volume24h: BigDecimal?,
    val liquidity: BigDecimal?,
    val orderIndex: Int,
    val outcomes: List<PolymarketOutcome>,
)