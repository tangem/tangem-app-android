package com.tangem.features.polymarket.impl.common

import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.utils.StringsSigns.PLUS
import java.math.BigDecimal
import java.math.RoundingMode

/** The amount, sign and direction share collateral precision, including values that round to zero. */
internal fun BigDecimal.toPolymarketPnl(): TangemPriceChange.State {
    val amount = setScale(PolymarketConst.COLLATERAL_DISPLAY_DECIMALS, RoundingMode.HALF_UP)
    val direction = when (amount.signum()) {
        1 -> TangemPriceChange.Direction.Up
        -1 -> TangemPriceChange.Direction.Down
        else -> TangemPriceChange.Direction.Neutral
    }
    val prefix = if (direction == TangemPriceChange.Direction.Up) PLUS else ""
    return TangemPriceChange.State(
        value = stringReference(prefix + amount.formatPolymarketMoney()),
        direction = direction,
    )
}