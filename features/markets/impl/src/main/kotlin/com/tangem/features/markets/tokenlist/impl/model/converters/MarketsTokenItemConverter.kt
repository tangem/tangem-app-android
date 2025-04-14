package com.tangem.features.markets.tokenlist.impl.model.converters

import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.charts.state.converter.PriceAndTimePointValuesConverter
import com.tangem.common.ui.charts.state.sorted
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarket
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListItemUM
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListUM.TrendInterval
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

internal class MarketsTokenItemConverter(
    private val currentTrendInterval: TrendInterval,
    private val appCurrency: AppCurrency,
) : Converter<TokenMarket, MarketsListItemUM> {

    private val priceAndTimePointValuesConverter = PriceAndTimePointValuesConverter(needToFormatAxis = false)

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
            isUnder100kMarketCap = value.isUnderMarketCapLimit,
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
            price = ifChanged(prev = prev.tokenQuotesShort, new = new.tokenQuotesShort, prevR = prevUI.price) {
                new.getCurrentPrice(
                    prev = prev,
                )
            },
            trendPercentText = ifChanged(
                prev.tokenQuotesShort,
                new.tokenQuotesShort,
                prevUI.trendPercentText,
            ) { new.getTrendPercent() },
            trendType = ifChanged(prev.tokenQuotesShort, new.tokenQuotesShort, prevUI.trendType) { new.getTrendType() },
            chardData = ifChanged(prev.tokenCharts, new.tokenCharts, prevUI.chardData) { new.getChartData() },
        )
    }

    private inline fun <T, R> ifChanged(prev: T, new: T, prevR: R, force: Boolean = false, change: (T) -> R): R {
        return if (force || prev != new) change(new) else prevR
    }

    private fun TokenMarket.getMarketCap(): String? {
        val value = marketCap?.takeIf { marketCap != BigDecimal.ZERO } ?: return null

        return value.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            ).compact(
                threeDigitsMethod = true,
            )
        }
    }

    private fun TokenMarket.getCurrentPrice(prev: TokenMarket? = null): MarketsListItemUM.Price {
        val prevPrice = prev?.tokenQuotesShort?.currentPrice

        val priceText = BigDecimalFormatter.formatFiatPriceUncapped(
            fiatAmount = tokenQuotesShort.currentPrice,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )

        val changeType = if (prevPrice != null) {
            if (tokenQuotesShort.currentPrice > prevPrice) {
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
            priceAndTimePointValuesConverter.convert(
                MarketChartData.Data(
                    y = ct.priceY.toImmutableList(),
                    x = ct.timeStamps.map { it.toBigDecimal() }.toImmutableList(),
                ).sorted(),
            )
        }
    }

    @Suppress("MagicNumber")
    private fun TokenMarket.getTrendType(): PriceChangeType {
        val percent = when (currentTrendInterval) {
            TrendInterval.H24 -> tokenQuotesShort.h24ChangePercent
            TrendInterval.D7 -> tokenQuotesShort.weekChangePercent
            TrendInterval.M1 -> tokenQuotesShort.monthChangePercent
        }
        val scaled = percent?.setScale(4, RoundingMode.HALF_UP)
        return when {
            scaled == null -> PriceChangeType.NEUTRAL
            scaled > BigDecimal.ZERO -> PriceChangeType.UP
            scaled < BigDecimal.ZERO -> PriceChangeType.DOWN
            else -> PriceChangeType.NEUTRAL
        }
    }

    private fun TokenMarket.getTrendPercent(): String {
        val percent = when (currentTrendInterval) {
            TrendInterval.H24 -> tokenQuotesShort.h24ChangePercent
            TrendInterval.D7 -> tokenQuotesShort.weekChangePercent
            TrendInterval.M1 -> tokenQuotesShort.monthChangePercent
        }

        return percent.format { percent() }
    }
}