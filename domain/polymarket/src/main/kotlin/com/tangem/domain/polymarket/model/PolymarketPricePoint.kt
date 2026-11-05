package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/** UTC milliseconds, converted from CLOB seconds at the data boundary. */
data class PolymarketPricePoint(val timestampMillis: Long, val probability: BigDecimal)