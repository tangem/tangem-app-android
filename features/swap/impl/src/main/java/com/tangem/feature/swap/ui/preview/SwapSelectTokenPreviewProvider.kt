package com.tangem.feature.swap.ui.preview

import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.token.AccountItemPreviewData
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.PortfolioItemContentUM
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.models.market.state.SwapMarketState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
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

    private val tokenItemState = TokenItemState.Content(
        id = "1",
        iconState = CurrencyIconState.Locked,
        titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Bitcoin")),
        fiatAmountState = TokenItemState.FiatAmountState.Content(text = "12 368,14 \$"),
        subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "0,35853044 BTC"),
        subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
            price = "34 496,75 \$",
            priceChangePercent = "0,43 %",
            type = PriceChangeType.DOWN,
        ),
        onItemClick = {},
        onItemLongClick = {},
    )

    private val textContentTokensState = persistentListOf(
        TokensListItemUM.GroupTitle(id = 111, text = stringReference("Network Bitcoin")),
        TokensListItemUM.Token(state = tokenItemState),
        TokensListItemUM.GroupTitle(id = 222, text = stringReference("Network Ethereum")),
        TokensListItemUM.Token(
            state = tokenItemState.copy(
                id = "2",
                titleState = TokenItemState.TitleState.Content(text = stringReference("Ethereum")),
                fiatAmountState = TokenItemState.FiatAmountState.Content(text = "3 340,79 \$"),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "1,856660295 ETH"),
                subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
                    price = "1 799,41 \$",
                    priceChangePercent = "5,16 %",
                    type = PriceChangeType.UP,
                ),
            ),
        ),
        TokensListItemUM.Token(
            state = TokenItemState.Unreachable(
                id = "3",
                iconState = CurrencyIconState.Locked,
                titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
                onItemClick = {},
                onItemLongClick = {},
            ),
        ),
        TokensListItemUM.Token(
            state = tokenItemState.copy(
                id = "4",
                titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Shiba Inu")),
                fiatAmountState = TokenItemState.FiatAmountState.Content(text = "48,64 \$"),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "6 200 220,00 SHIB"),
                subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
                    price = "0.01 \$",
                    priceChangePercent = "1,34 %",
                    type = PriceChangeType.DOWN,
                ),
            ),
        ),
    )

    private val marketState = SwapMarketState.Content(
        items = createPreviewMarketItems(),
        loadMore = { },
        onItemClick = { },
        visibleIdsChanged = { },
        total = TOTAL_ITEMS,
        marketsTitle = TextReference.Res(R.string.feed_trending_now),
        shouldAssetsCount = false,
    )

    val defaultState = SwapSelectTokenStateHolder(
        tokensListData = TokenListUMData.AccountList(
            tokensList = persistentListOf(
                TokensListItemUM.Portfolio(
                    content = PortfolioItemContentUM.Tokens(
                        tokens = textContentTokensState.filterIsInstance<PortfolioTokensListItemUM>()
                            .toPersistentList(),
                    ),
                    isExpanded = false,
                    isCollapsable = true,
                    tokenItemUM = AccountItemPreviewData.accountItem
                        .copy(iconState = AccountItemPreviewData.accountLetterIcon),
                ),
                TokensListItemUM.Portfolio(
                    content = PortfolioItemContentUM.Tokens(
                        tokens = textContentTokensState.filterIsInstance<PortfolioTokensListItemUM>()
                            .toPersistentList(),
                    ),
                    isExpanded = true,
                    isCollapsable = true,
                    tokenItemUM = AccountItemPreviewData.accountItem,
                ),
            ),
            totalTokensCount = TOTAL_ITEMS,
        ),
        isAfterSearch = false,
        isBalanceHidden = false,
        onSearchEntered = {},
        marketsState = marketState,
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