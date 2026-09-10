package com.tangem.features.feed.rwa.model.list

import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarket
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Presentation of a [TokenMarket] shared by the tab's list rows and its Featured rail.
 *
 * The response carries the change percent for every interval at once, so switching the tab's trend
 * interval is a re-read of the already-loaded token, never a new request.
 */
internal fun TokenMarket.trendChangePercent(interval: RwaInterval): BigDecimal? {
    return when (interval) {
        RwaInterval.H24 -> tokenQuotesShort.h24ChangePercent
        RwaInterval.D7 -> tokenQuotesShort.weekChangePercent
        RwaInterval.M1 -> tokenQuotesShort.monthChangePercent
    }
}

@Suppress("MagicNumber")
internal fun BigDecimal?.toPriceChangeDirection(): TangemPriceChange.Direction {
    val scaled = this?.setScale(4, RoundingMode.HALF_UP)

    return when {
        scaled == null -> TangemPriceChange.Direction.Neutral
        scaled > BigDecimal.ZERO -> TangemPriceChange.Direction.Up
        scaled < BigDecimal.ZERO -> TangemPriceChange.Direction.Down
        else -> TangemPriceChange.Direction.Neutral
    }
}

internal fun TokenMarket.formatPrice(appCurrency: AppCurrency): String {
    return tokenQuotesShort.currentPrice.format {
        fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol).price()
    }
}