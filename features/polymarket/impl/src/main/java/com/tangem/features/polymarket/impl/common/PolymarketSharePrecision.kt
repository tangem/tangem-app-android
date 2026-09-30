package com.tangem.features.polymarket.impl.common

import java.math.BigDecimal
import java.math.RoundingMode

/** @return this share count cut down to the on-chain share precision, never rounded up past what is held */
internal fun BigDecimal.floorToSharePrecision(): BigDecimal = if (scale() > PolymarketConst.SHARES_DECIMALS) {
    setScale(PolymarketConst.SHARES_DECIMALS, RoundingMode.FLOOR)
} else {
    this
}