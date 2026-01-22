package com.tangem.features.feed.model.news.details.converter

import androidx.compose.runtime.Stable
import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.charts.state.converter.PriceAndTimePointValuesConverter
import com.tangem.common.ui.charts.state.sorted
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenChart
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

@Stable
internal class RelatedTokenConverter(private val appCurrency: AppCurrency) :
    Converter<Pair<TokenMarketInfo, TokenChart?>, MarketsListItemUM> {

    private val priceAndTimePointValuesConverter = PriceAndTimePointValuesConverter(shouldFormatAxis = false)

    override fun convert(value: Pair<TokenMarketInfo, TokenChart?>): MarketsListItemUM {
        val (tokenInfo, chart) = value
        val tokenId = CryptoCurrency.RawID(tokenInfo.id)

        return MarketsListItemUM(
            id = tokenId,
            name = tokenInfo.name,
            currencySymbol = tokenInfo.symbol,
            ratingPosition = tokenInfo.metrics?.marketRating?.toString(),
            marketCap = getMarketCap(tokenInfo),
            iconUrl = getTokenIconUrlFromDefaultHost(tokenId),
            price = getCurrentPrice(tokenInfo),
            trendPercentText = getTrendPercent(tokenInfo),
            trendType = getTrendType(tokenInfo),
            chartData = getChartData(chart),
            isUnder100kMarketCap = tokenInfo.metrics?.marketCap?.let { it < BigDecimal(MARKET_CAP_100K) } == true,
            stakingRate = null,
            updateTimestamp = null,
        )
    }

    private fun getMarketCap(tokenInfo: TokenMarketInfo): String? {
        val value = tokenInfo.metrics?.marketCap?.takeIf { it != BigDecimal.ZERO } ?: return null

        return value.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            ).compact(
                threeDigitsMethod = true,
            )
        }
    }

    private fun getCurrentPrice(tokenInfo: TokenMarketInfo): MarketsListItemUM.Price {
        val priceText = tokenInfo.quotes.currentPrice.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            ).price()
        }

        return MarketsListItemUM.Price(
            text = priceText,
            changeType = null,
        )
    }

    private fun getChartData(chart: TokenChart?): MarketChartRawData? {
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
    private fun getTrendType(tokenInfo: TokenMarketInfo): PriceChangeType {
        val scaled = tokenInfo.quotes.h24ChangePercent?.setScale(4, RoundingMode.HALF_UP)
        return when {
            scaled == null -> PriceChangeType.NEUTRAL
            scaled > BigDecimal.ZERO -> PriceChangeType.UP
            scaled < BigDecimal.ZERO -> PriceChangeType.DOWN
            else -> PriceChangeType.NEUTRAL
        }
    }

    private fun getTrendPercent(tokenInfo: TokenMarketInfo): String {
        return tokenInfo.quotes.h24ChangePercent?.format { percent() } ?: "0%"
    }

    companion object {
        private const val MARKET_CAP_100K = "100000"
    }
}