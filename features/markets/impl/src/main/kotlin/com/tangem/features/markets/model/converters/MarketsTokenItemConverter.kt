package com.tangem.features.markets.model.converters

import com.tangem.common.ui.charts.state.DefaultPointValuesConverter
import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarket
import com.tangem.features.markets.ui.entity.MarketsListItemUM
import com.tangem.features.markets.ui.entity.MarketsListUM.TrendInterval
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode

internal class MarketsTokenItemConverter(
    private val currentTrendInterval: TrendInterval,
    private val appCurrency: AppCurrency,
) : Converter<TokenMarket, MarketsListItemUM> {

    override fun convert(value: TokenMarket): MarketsListItemUM {
        return MarketsListItemUM(
            id = value.id,
            name = value.name,
            currencySymbol = value.symbol,
            ratingPosition = value.marketRating?.toString(),
            marketCap = value.getMarketCap(),
            iconUrl = value.imageUrlLarge,
            price = value.getCurrentPrice(),
            trendPercentText = value.getTrendPercent(),
            trendType = value.getTrendType(),
            chardData = value.getChartData(),
        )
    }

    fun update(prev: TokenMarket, prevUI: MarketsListItemUM, new: TokenMarket): MarketsListItemUM {
        require(prev.id == new.id) {
            "Ids is not the same during update TokenMarket item: previousItem[${prev.id}] != newItem[${new.id}]"
        }

        return prevUI.copy(
            name = new.name,
            currencySymbol = new.symbol,
            ratingPosition = new.marketRating?.toString(),
            marketCap = ifChanged(prev.marketCap, new.marketCap, prevUI.marketCap) { new.getMarketCap() },
            iconUrl = new.imageUrlLarge,
            price = ifChanged(prev = prev.tokenQuotes, new = new.tokenQuotes, prevR = prevUI.price) {
                new.getCurrentPrice(
                    prev = prev,
                )
            },
            trendPercentText = ifChanged(
                prev.tokenQuotes,
                new.tokenQuotes,
                prevUI.trendPercentText,
            ) { new.getTrendPercent() },
            trendType = ifChanged(prev.tokenQuotes, new.tokenQuotes, prevUI.trendType) { new.getTrendType() },
            chardData = ifChanged(prev.tokenCharts, new.tokenCharts, prevUI.chardData) { new.getChartData() },
        )
    }

    private inline fun <T, R> ifChanged(prev: T, new: T, prevR: R, force: Boolean = false, change: (T) -> R): R {
        return if (force || prev != new) change(new) else prevR
    }

    @Suppress("UnnecessaryParentheses", "MagicNumber")
    private fun TokenMarket.getMarketCap(): String? {
        val value = marketCap?.setScale(0, RoundingMode.HALF_UP)?.toLong() ?: return null

        val formatted = when {
            value > 1_000_000_000_000L -> {
                val trillion = value / 1_000_000_000_000
                val billion = (value % 1_000_000_000_000) / 1_000_000_000
                "$trillion.${billion}T"
            }
            value > 1_000_000_000L -> {
                val billion = value / 1_000_000_000
                val million = (value % 1_000_000_000) / 1_000_000
                "$billion.${million}B"
            }
            value > 1_000_000L -> {
                val million = value / 1_000_000
                val thousand = (value % 1_000_000) / 1_000
                "$million.${thousand}M"
            }
            value > 1_000L -> {
                val thousand = value / 1_000
                "${thousand}K"
            }
            else -> return value.toString()
        }

        return BigDecimalFormatter
            .formatStringAmount(
                formatted,
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
    }

    private fun TokenMarket.getCurrentPrice(prev: TokenMarket? = null): MarketsListItemUM.Price {
        val prevPrice = prev?.tokenQuotes?.currentPrice

        val priceText = BigDecimalFormatter.formatFiatAmount(
            fiatAmount = tokenQuotes.currentPrice,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )

        val changeType = if (prevPrice != null) {
            if (tokenQuotes.currentPrice > prevPrice) {
                PriceChangeType.UP
            } else {
                PriceChangeType.DOWN
            }
        } else {
            null
        }

        return MarketsListItemUM.Price(
            text = priceText,
            changeType = changeType,
        )
    }

    private fun TokenMarket.getChartData(): MarketChartRawData? {
        val chart = when (currentTrendInterval) {
            TrendInterval.H24 -> tokenCharts.h24
            TrendInterval.D7 -> tokenCharts.week
            TrendInterval.M1 -> tokenCharts.month
        }

        return chart?.let { ct ->
            DefaultPointValuesConverter.convert(
                MarketChartData.Data(
                    y = ct.priceY,
                    x = ct.timeStamp.map { it.toBigDecimal() },
                ),
            )
        }
    }

    private fun TokenMarket.getTrendType(): PriceChangeType {
        val percent = when (currentTrendInterval) {
            TrendInterval.H24 -> tokenQuotes.h24Percent()
            TrendInterval.D7 -> tokenQuotes.weekPercent()
            TrendInterval.M1 -> tokenQuotes.monthPercent()
        }.setScale(2, RoundingMode.UP)

        return when (percent.compareTo(BigDecimal.ZERO)) {
            1 -> PriceChangeType.UP
            -1 -> PriceChangeType.DOWN
            else -> PriceChangeType.NEUTRAL
        }
    }

    private fun TokenMarket.getTrendPercent(): String {
        val percent = when (currentTrendInterval) {
            TrendInterval.H24 -> tokenQuotes.h24Percent()
            TrendInterval.D7 -> tokenQuotes.weekPercent()
            TrendInterval.M1 -> tokenQuotes.monthPercent()
        }

        return BigDecimalFormatter.formatPercent(
            percent = percent,
            useAbsoluteValue = true,
        )
    }
}
