package com.tangem.common.ui.tokens

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import java.math.BigDecimal

fun BigDecimal.toPriceChangeState(): TangemPriceChange.State = TangemPriceChange.State(
    value = stringReference(format { percent() }),
    direction = when (PriceChangeType.fromBigDecimal(this)) {
        PriceChangeType.UP -> TangemPriceChange.Direction.Up
        PriceChangeType.DOWN -> TangemPriceChange.Direction.Down
        PriceChangeType.NEUTRAL -> TangemPriceChange.Direction.Neutral
    },
)

fun CurrencyIconState.toTokenIconState(): TangemTokenIcon.UiState = when (this) {
    is CurrencyIconState.CoinIcon -> tokenIcon(url = url)
    is CurrencyIconState.TokenIcon -> tokenIcon(url = url)
    is CurrencyIconState.CustomTokenIcon -> tokenIcon(url = null)
    CurrencyIconState.Loading -> TangemTokenIcon.UiState.Shimmer
    else -> TangemTokenIcon.UiState.Error
}

private fun CurrencyIconState.tokenIcon(url: String?): TangemTokenIcon.UiState = TangemTokenIcon.UiState.Token(
    TangemTokenIcon.State(
        url = url,
        topIcon = topBadgeIconResId,
        isGrayscale = isGrayscale,
        indicator = TangemTokenIcon.State.Indicator().takeIf { shouldShowCustomBadge },
    ),
)