package com.tangem.features.feed.model.market.details.formatter

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenQuotes
import java.math.BigDecimal
import java.math.RoundingMode

internal fun TokenQuotes.getFormattedPercentByInterval(interval: PriceChangeInterval): String {
    val percent = when (interval) {
        PriceChangeInterval.H24 -> h24ChangePercent
        PriceChangeInterval.WEEK -> weekChangePercent
        PriceChangeInterval.MONTH -> monthChangePercent
        PriceChangeInterval.MONTH3 -> m3ChangePercent
        PriceChangeInterval.MONTH6 -> m6ChangePercent
        PriceChangeInterval.YEAR -> yearChangePercent
        PriceChangeInterval.ALL_TIME -> allTimeChangePercent
    }

    return percent?.format { percent() }.orEmpty()
}

internal fun TokenQuotes.getPercentByInterval(interval: PriceChangeInterval): BigDecimal? {
    return when (interval) {
        PriceChangeInterval.H24 -> h24ChangePercent
        PriceChangeInterval.WEEK -> weekChangePercent
        PriceChangeInterval.MONTH -> monthChangePercent
        PriceChangeInterval.MONTH3 -> m3ChangePercent
        PriceChangeInterval.MONTH6 -> m6ChangePercent
        PriceChangeInterval.YEAR -> yearChangePercent
        PriceChangeInterval.ALL_TIME -> allTimeChangePercent
    }
}

@Suppress("MagicNumber")
internal fun BigDecimal?.percentChangeType(): PriceChangeType {
    val scaled = this?.setScale(4, RoundingMode.HALF_UP)
    return when {
        scaled == null -> PriceChangeType.NEUTRAL
        scaled > BigDecimal.ZERO -> PriceChangeType.UP
        scaled < BigDecimal.ZERO -> PriceChangeType.DOWN
        else -> PriceChangeType.NEUTRAL
    }
}

@Suppress("MagicNumber")
internal fun getChangePercentBetween(currentPrice: BigDecimal, previousPrice: BigDecimal): BigDecimal {
    return if (previousPrice == BigDecimal.ZERO) {
        BigDecimal.ZERO
    } else {
        currentPrice.subtract(previousPrice).divide(previousPrice, 4, RoundingMode.HALF_UP)
    }
}

internal fun getFormattedPriceChange(currentPrice: BigDecimal, updatedPrice: BigDecimal): PriceChangeType {
    val current = getFiatPriceAmountWithScale(value = currentPrice).first
    val updated = getFiatPriceAmountWithScale(value = updatedPrice).first

    return when {
        updated > current -> PriceChangeType.UP
        updated < current -> PriceChangeType.DOWN
        else -> PriceChangeType.NEUTRAL
    }
}

internal fun PriceChangeType.toChartType(): MarketChartLook.Type {
    return when (this) {
        PriceChangeType.UP -> MarketChartLook.Type.Growing
        PriceChangeType.DOWN -> MarketChartLook.Type.Falling
        PriceChangeType.NEUTRAL -> MarketChartLook.Type.Neutral
    }
}

internal fun BigDecimal.toMarketsTokenDetailsPriceAnnotated(appCurrency: AppCurrency): TextReference {
    return formatStyled {
        fiat(
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
            spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.tertiary) },
        ).price()
    }
}