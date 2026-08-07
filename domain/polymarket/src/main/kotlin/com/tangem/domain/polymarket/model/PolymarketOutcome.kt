package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * A single normalized outcome of a [PolymarketMarket] (BFF `OutcomeSummary`).
 *
 * @property assetId upstream asset (clob token) id of the outcome
 * @property title outcome label as provided by upstream (e.g. "Yes" / "No"), never hardcoded
 * @property probability implied probability in the 0..1 range, if known
 */
data class PolymarketOutcome(
    val assetId: String,
    val title: String,
    val probability: BigDecimal?,
)