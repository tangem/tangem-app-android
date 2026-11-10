package com.tangem.features.polymarket.impl.common

import com.tangem.domain.polymarket.model.PolymarketMarket
import java.time.Instant

internal fun PolymarketMarket.endInstantOrNull(): Instant? {
    val raw = endDate ?: return null
    return runCatching { Instant.parse(raw) }.getOrNull()
}