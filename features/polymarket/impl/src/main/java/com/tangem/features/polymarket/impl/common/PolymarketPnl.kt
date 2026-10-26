package com.tangem.features.polymarket.impl.common

import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.utils.StringsSigns.PLUS
import java.math.BigDecimal
import java.math.RoundingMode

/** The amount, sign and direction share collateral precision, including values that round to zero. */
internal fun BigDecimal.toPolymarketPnl(): TangemPriceChange.State {
    val amount = setScale(PolymarketConst.COLLATERAL_DISPLAY_DECIMALS, RoundingMode.HALF_UP)
    val direction = amount.toPolymarketPnlDirection()
    val prefix = if (direction == TangemPriceChange.Direction.Up) PLUS else ""
    return TangemPriceChange.State(
        value = stringReference(prefix + amount.formatPolymarketMoney()),
        direction = direction,
    )
}

/** Position cards treat changes below one collateral display unit as neutral before rounding. */
internal fun BigDecimal.toPolymarketPnlDirection(): TangemPriceChange.Direction = when {
    abs() < PolymarketConst.COLLATERAL_DISPLAY_STEP -> TangemPriceChange.Direction.Neutral
    signum() > 0 -> TangemPriceChange.Direction.Up
    else -> TangemPriceChange.Direction.Down
}