package com.tangem.features.markets.details.impl.model.formatter

import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenQuotes
import java.math.BigDecimal
import java.math.RoundingMode

internal fun BigDecimal.formatAsPrice(currency: AppCurrency): String {
    return BigDecimalFormatter.formatFiatPriceUncapped(
        fiatAmount = this,
        fiatCurrencyCode = currency.code,
        fiatCurrencySymbol = currency.symbol,
    )
}

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

    return percent?.let {
        BigDecimalFormatter.formatPercent(
            percent = it,
            useAbsoluteValue = true,
        )
    } ?: ""
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

internal fun BigDecimal?.percentChangeType(): PriceChangeType {
    return when {
        this == null -> PriceChangeType.NEUTRAL
        this > BigDecimal.ZERO -> PriceChangeType.UP
        this < BigDecimal.ZERO -> PriceChangeType.DOWN
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
    val updatedPriceDecimals = BigDecimalFormatter.getProperFiatPriceDecimals(updatedPrice)

    val current = currentPrice.setScale(updatedPriceDecimals, RoundingMode.HALF_UP)
    val updated = updatedPrice.setScale(updatedPriceDecimals, RoundingMode.HALF_UP)

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
        PriceChangeType.NEUTRAL -> MarketChartLook.Type.Growing
    }
}

internal fun getChartTypeByPercent(percent: BigDecimal?): MarketChartLook.Type {
    return when {
        percent == null -> return MarketChartLook.Type.Growing
        percent == BigDecimal.ZERO -> MarketChartLook.Type.Growing
        percent > BigDecimal.ZERO -> MarketChartLook.Type.Growing
        percent < BigDecimal.ZERO -> MarketChartLook.Type.Falling
        else -> MarketChartLook.Type.Growing
    }
}