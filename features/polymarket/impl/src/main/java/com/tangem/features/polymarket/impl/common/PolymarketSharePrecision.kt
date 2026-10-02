package com.tangem.features.polymarket.impl.common

import com.tangem.domain.polymarket.approval.PolymarketContracts
import java.math.BigDecimal
import java.math.RoundingMode

/** @return this share count cut down to the on-chain share precision, never rounded up past what is held */
internal fun BigDecimal.floorToSharePrecision(): BigDecimal = if (scale() > PolymarketContracts.SHARE_DECIMALS) {
    setScale(PolymarketContracts.SHARE_DECIMALS, RoundingMode.FLOOR)
} else {
    this
}