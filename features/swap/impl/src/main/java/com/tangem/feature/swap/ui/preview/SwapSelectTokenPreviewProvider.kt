package com.tangem.feature.swap.ui.preview

import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.models.TokenToSelectState
import com.tangem.feature.swap.models.market.state.SwapMarketState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class SwapSelectTokenPreviewProvider {

    fun provideSwapSelectTokenState(): SwapSelectTokenStateHolder {
        return SwapSelectTokenStateHolder(
            availableTokens = listOf(previewTitle, previewToken, previewToken, previewToken).toImmutableList(),
            unavailableTokens = listOf(previewTitle, previewToken, previewToken, previewToken).toImmutableList(),
            tokensListData = TokenListUMData.EmptyList,
            isAfterSearch = false,
            isBalanceHidden = false,
            onSearchEntered = {},
            onTokenSelected = {},
            marketsState = createPreviewMarketsState(),
        )
    }

    private fun createPreviewMarketsState() = SwapMarketState.Content(
        items = createPreviewMarketItems(),
        loadMore = { },
        onItemClick = { },
        visibleIdsChanged = { },
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
        price = MarketsListItemUM.Price(text = "31 285.72$"),
        trendPercentText = "12.43%",
        trendType = trendType,
        chartData = chartData,
        isUnder100kMarketCap = false,
        stakingRate = stringReference("APY 12.34%"),
        updateTimestamp = 0,
    )

    companion object {
        private const val CHART_VALUE_1 = 0.4
        private const val CHART_VALUE_2 = 0.2
        private const val CHART_VALUE_3 = 0.1
        private const val CHART_VALUE_4 = 2.0
        private const val CHART_VALUE_5 = 5.0
        private const val CHART_VALUE_6 = 3.0

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

        private val previewToken = TokenToSelectState.TokenToSelect(
            tokenIcon = CurrencyIconState.CoinIcon(
                url = "",
                fallbackResId = 0,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
            id = "",
            name = "Optimistic Ethereum (ETH)",
            symbol = "USDC",
            addedTokenBalanceData = TokenBalanceData(
                amount = "15 000 $",
                amountEquivalent = "15 000 USDT",
                isBalanceHidden = false,
            ),
        )

        private val previewTitle = TokenToSelectState.Title(
            title = stringReference("MY TOKENS"),
        )
    }
}