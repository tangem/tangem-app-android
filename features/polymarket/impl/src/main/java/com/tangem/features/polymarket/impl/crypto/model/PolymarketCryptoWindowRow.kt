package com.tangem.features.polymarket.impl.crypto.model

import com.tangem.domain.polymarket.model.PolymarketCryptoWindowPhase
import java.time.Instant

/** A window of an Up/Down series; each window is an event of its own. */
internal data class PolymarketCryptoWindowTab(
    val eventId: String,
    val start: Instant,
    val end: Instant,
) {

    fun phaseAt(nowMillis: Long): PolymarketCryptoWindowPhase = PolymarketCryptoWindowPhase.at(start, end, nowMillis)
}

/**
 * @property tabs windows to choose from, earliest first
 * @property selectedId tab to mark as chosen, always one of [tabs]
 */
internal data class PolymarketCryptoWindowRow(
    val tabs: List<PolymarketCryptoWindowTab>,
    val selectedId: String,
)