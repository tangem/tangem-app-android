package com.tangem.features.polymarket.impl.common

import com.tangem.domain.polymarket.model.PolymarketMarket
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun PolymarketMarket.endInstantOrNull(): Instant? {
    val raw = endDate ?: return null
    return runCatching { Instant.parse(raw) }.getOrNull()
}

internal fun PolymarketMarket.easternEndDateOrNull(): LocalDate? =
    endInstantOrNull()?.atZone(ZoneId.of(PolymarketConst.EASTERN_TIME_ZONE))?.toLocalDate()