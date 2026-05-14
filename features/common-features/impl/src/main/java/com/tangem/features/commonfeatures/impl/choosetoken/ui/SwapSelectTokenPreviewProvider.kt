package com.tangem.features.commonfeatures.impl.choosetoken.ui

import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal object SwapSelectTokenPreviewProvider {

    private const val CHART_VALUE_1 = 0.4
    private const val CHART_VALUE_2 = 0.2
    private const val CHART_VALUE_3 = 0.1
    private const val CHART_VALUE_4 = 2.0
    private const val CHART_VALUE_5 = 5.0
    private const val CHART_VALUE_6 = 3.0
    private const val TOTAL_ITEMS = 322

    private val PREVIEW_CHART_DATA = MarketChartRawData(
        y = persistentListOf(
            CHART_VALUE_1,
            CHART_VALUE_2,
            CHART_VALUE_1,
            CHART_VALUE_3,
            CHART_VALUE_1,
            CHART_VALUE_4,
            CHART_VALUE_5,
            CHART_VALUE_3,
            CHART_VALUE_4,
            CHART_VALUE_4,
            CHART_VALUE_6,
        ),
    )

    val marketState = SwapMarketState.Content(
        items = createPreviewMarketItems(),
        loadMore = { },
        onItemClick = { },
        visibleIdsChanged = { },
        total = TOTAL_ITEMS,
        marketsTitle = TextReference.Res(R.string.feed_trending_now),
        shouldAssetsCount = false,
    )

    private fun createPreviewMarketItems() = listOf(
        createMarketItem(
            id = "1",
            iconUrl = "",
            ratingPosition = "10",
            marketCap = "$6.233 B",
            trendType = PriceChangeType.UP,
            chartData = PREVIEW_CHART_DATA,
        ),
        createMarketItem(
            id = "2",
            ratingPosition = "10",
            marketCap = "$6.233 B",
            trendType = PriceChangeType.NEUTRAL,
            chartData = null,
        ),
        createMarketItem(
            id = "3",
            name = "Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin",
            ratingPosition = "10",
            marketCap = "$6.23348172384781234 B",
            trendType = PriceChangeType.DOWN,
            chartData = PREVIEW_CHART_DATA,
        ),
        createMarketItem(
            id = "4",
            ratingPosition = "10",
            marketCap = null,
            trendType = PriceChangeType.UP,
            chartData = PREVIEW_CHART_DATA,
        ),
        createMarketItem(
            id = "5",
            ratingPosition = null,
            marketCap = "$6.233 B",
            trendType = PriceChangeType.UP,
            chartData = PREVIEW_CHART_DATA,
        ),
        createMarketItem(
            id = "6",
            ratingPosition = null,
            marketCap = null,
            trendType = PriceChangeType.UP,
            chartData = PREVIEW_CHART_DATA,
        ),
    ).toImmutableList()

    private fun createMarketItem(
        id: String,
        name: String = "Bitcoin",
        iconUrl: String? = null,
        ratingPosition: String?,
        marketCap: String?,
        trendType: PriceChangeType,
        chartData: MarketChartRawData?,
    ) = MarketsListItemUM(
        id = CryptoCurrency.RawID(id),
        name = name,
        currencySymbol = "BTC",
        iconUrl = iconUrl,
        ratingPosition = ratingPosition,
        marketCap = marketCap,
        price = MarketsListItemUM.Price(
            text = "31 285.72$",
            annotated = stringReference("31 285.72$"),
            fiatPrice = BigDecimal("123123"),
        ),
        trendPercentText = "12.43%",
        trendType = trendType,
        chartData = chartData,
        isUnder100kMarketCap = false,
        stakingRate = stringReference("APY 12.34%"),
        updateTimestamp = 0,
    )
}