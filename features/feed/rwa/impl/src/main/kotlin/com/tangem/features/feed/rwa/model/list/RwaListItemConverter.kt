package com.tangem.features.feed.rwa.model.list

import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.charts.state.converter.PriceAndTimePointValuesConverter
import com.tangem.common.ui.charts.state.sorted
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.rwa.ui.state.RwaListItemUM
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/**
 * Port of the old feed's `MarketsTokenItemConverter` onto the new DS component.
 */
internal class RwaListItemConverter(
    private val currentTrendInterval: RwaInterval,
    private val appCurrency: AppCurrency,
    private val onItemClick: (CryptoCurrency.RawID) -> Unit,
) {

    private val priceAndTimePointValuesConverter = PriceAndTimePointValuesConverter(shouldFormatAxis = false)

    fun convert(value: TokenMarket): RwaListItemUM {
        return RwaListItemUM(
            row = value.toRowState(priceUpdateDirection = null),
            chartData = value.getChartData(),
            chartType = value.getTrendDirection().toChartType(),
        )
    }

    fun update(prev: TokenMarket, prevUI: RwaListItemUM, new: TokenMarket): RwaListItemUM {
        require(prev.id == new.id) {
            "Ids is not the same during update TokenMarket item: previousItem[${prev.id}] != newItem[${new.id}]"
        }

        val prevPrice = prev.tokenQuotesShort.currentPrice
        val newPrice = new.tokenQuotesShort.currentPrice
        // flash the price in the direction color only on a live quote change
        val priceUpdateDirection = when {
            newPrice > prevPrice -> TangemPriceChange.Direction.Up
            newPrice < prevPrice -> TangemPriceChange.Direction.Down
            else -> prevUI.row.priceUpdateDirection
        }

        return RwaListItemUM(
            row = new.toRowState(priceUpdateDirection = priceUpdateDirection),
            chartData = ifChanged(prev.tokenCharts, new.tokenCharts, prevUI.chartData) { new.getChartData() },
            chartType = new.getTrendDirection().toChartType(),
        )
    }

    private fun TokenMarket.toRowState(
        priceUpdateDirection: TangemPriceChange.Direction?,
    ): TangemTokenRowMarket.State.Content {
        val trendDirection = getTrendDirection()

        return TangemTokenRowMarket.State.Content(
            id = id.value,
            icon = TangemTokenIcon.UiState.Token(
                TangemTokenIcon.State(url = imageUrlLarge, indicator = restrictionIndicator()),
            ),
            title = stringReference(name),
            ticker = stringReference(symbol),
            position = marketRating?.toString()?.let(::stringReference),
            capitalization = getMarketCap()?.let(::stringReference),
            price = stringReference(formatPrice(appCurrency)),
            priceChange = TangemPriceChange.State(
                value = stringReference(getTrendPercent()),
                direction = trendDirection,
            ),
            priceUpdateDirection = priceUpdateDirection,
            onClick = { onItemClick(id) },
        )
    }

    private inline fun <T, R> ifChanged(prev: T, new: T, prevR: R, change: (T) -> R): R {
        return if (prev != new) change(new) else prevR
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

    private fun TokenMarket.getChartData(): MarketChartRawData? {
        val chart = when (currentTrendInterval) {
            RwaInterval.H24 -> tokenCharts.h24
            RwaInterval.D7 -> tokenCharts.week
            RwaInterval.M1 -> tokenCharts.month
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

    private fun TokenMarket.getTrendPercent(): String {
        return trendChangePercent(currentTrendInterval).format { percent() }
    }

    private fun TokenMarket.getTrendDirection(): TangemPriceChange.Direction {
        return trendChangePercent(currentTrendInterval).toPriceChangeDirection()
    }

    private fun TangemPriceChange.Direction.toChartType(): MarketChartLook.Type {
        return when (this) {
            TangemPriceChange.Direction.Up -> MarketChartLook.Type.Growing
            TangemPriceChange.Direction.Down -> MarketChartLook.Type.Falling
            TangemPriceChange.Direction.Neutral -> MarketChartLook.Type.Neutral
        }
    }
}