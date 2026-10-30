package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * A single normalized outcome of a [PolymarketMarket] (BFF `OutcomeSummary`).
 *
 * @property assetId upstream asset id of the outcome
 * @property title outcome label as provided by upstream (e.g. "Yes" / "No"), never hardcoded
 * @property probability implied probability in the 0..1 range, if known
 * @property legacyAssetId CTF token the outcome had before its market moved to V2; matches positions, never trades.
 *  May be empty when upstream position ids are broken
 */
data class PolymarketOutcome(
    val assetId: String,
    val title: String,
    val probability: BigDecimal?,
    val legacyAssetId: String?,
)