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
            marketsState = SwapMarketState.Content(
                items = listOf(
                    MarketsListItemUM(
                        id = CryptoCurrency.RawID("1"),
                        name = "Bitcoin",
                        currencySymbol = "BTC",
                        iconUrl = "",
                        ratingPosition = "10",
                        marketCap = "$6.233 B",
                        price = MarketsListItemUM.Price(text = "31 285.72$"),
                        trendPercentText = "12.43%",
                        trendType = PriceChangeType.UP,
                        chartData = MarketChartRawData(
                            y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
                        ),
                        isUnder100kMarketCap = false,
                        stakingRate = stringReference("APY 12.34%"),
                        updateTimestamp = 0,
                    ),
                    MarketsListItemUM(
                        id = CryptoCurrency.RawID("2"),
                        name = "Bitcoin",
                        currencySymbol = "BTC",
                        iconUrl = null,
                        ratingPosition = "10",
                        marketCap = "$6.233 B",
                        price = MarketsListItemUM.Price(text = "31 285.72$"),
                        trendPercentText = "12.43%",
                        trendType = PriceChangeType.NEUTRAL,
                        chartData = null,
                        isUnder100kMarketCap = false,
                        stakingRate = stringReference("APY 12.34%"),
                        updateTimestamp = 0,
                    ),
                    MarketsListItemUM(
                        id = CryptoCurrency.RawID("3"),
                        name = "Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin",
                        currencySymbol = "BTC",
                        iconUrl = null,
                        ratingPosition = "10",
                        marketCap = "$6.23348172384781234 B",
                        price = MarketsListItemUM.Price(text = "31 285.72$"),
                        trendPercentText = "12.43%",
                        trendType = PriceChangeType.DOWN,
                        chartData = MarketChartRawData(
                            y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
                        ),
                        isUnder100kMarketCap = false,
                        stakingRate = stringReference("APY 12.34%"),
                        updateTimestamp = 0,
                    ),
                    MarketsListItemUM(
                        id = CryptoCurrency.RawID("4"),
                        name = "Bitcoin",
                        currencySymbol = "BTC",
                        iconUrl = null,
                        ratingPosition = "10",
                        marketCap = null,
                        price = MarketsListItemUM.Price(text = "31 285.72$"),
                        trendPercentText = "12.43%",
                        trendType = PriceChangeType.UP,
                        chartData = MarketChartRawData(
                            y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
                        ),
                        isUnder100kMarketCap = false,
                        stakingRate = stringReference("APY 12.34%"),
                        updateTimestamp = 0,
                    ),
                    MarketsListItemUM(
                        id = CryptoCurrency.RawID("5"),
                        name = "Bitcoin",
                        currencySymbol = "BTC",
                        iconUrl = null,
                        ratingPosition = null,
                        marketCap = "$6.233 B",
                        price = MarketsListItemUM.Price(text = "31 285.72$"),
                        trendPercentText = "12.43%",
                        trendType = PriceChangeType.UP,
                        chartData = MarketChartRawData(
                            y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
                        ),
                        isUnder100kMarketCap = false,
                        stakingRate = stringReference("APY 12.34%"),
                        updateTimestamp = 0,
                    ),
                    MarketsListItemUM(
                        id = CryptoCurrency.RawID("6"),
                        name = "Bitcoin",
                        currencySymbol = "BTC",
                        iconUrl = null,
                        ratingPosition = null,
                        marketCap = null,
                        price = MarketsListItemUM.Price(text = "31 285.72$"),
                        trendPercentText = "12.43%",
                        trendType = PriceChangeType.UP,
                        chartData = MarketChartRawData(
                            y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
                        ),
                        isUnder100kMarketCap = false,
                        stakingRate = stringReference("APY 12.34%"),
                        updateTimestamp = 0,
                    ),
                ).toImmutableList(),
                loadMore = { true },
                onItemClick = { },
            ),
        )
    }

    companion object {
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