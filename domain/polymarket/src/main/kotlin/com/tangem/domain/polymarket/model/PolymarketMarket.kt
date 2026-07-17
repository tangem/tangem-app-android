package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * A single Polymarket prediction market nested inside a [PolymarketEvent] (BFF `Market`).
 *
 * @property id unique market id
 * @property title the market question (e.g. "Will X happen by 2026?")
 * @property iconUrl optional market icon
 * @property volume total traded volume in USD, if known
 * @property outcomes normalized outcomes with their implied probabilities
 */
data class PolymarketMarket(
    val id: String,
    val title: String,
    val iconUrl: String?,
    val volume: BigDecimal?,
    val outcomes: List<PolymarketOutcome>,
)