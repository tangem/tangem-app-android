package com.tangem.features.feed.ui.feed.preview

import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.ui.feed.state.*
import com.tangem.features.feed.ui.market.list.state.SortByTypeUM
import kotlinx.collections.immutable.*

@Suppress("MagicNumber")
internal object FeedListPreviewDataProvider {

    fun createFeedPreviewState(): FeedListUM {
        val articles = createSampleArticles()
        val marketItems = createSampleMarketItems()
        return FeedListUM(
            currentDate = "20 November",
            feedListSearchBar = FeedListSearchBar(
                placeholderText = TextReference.Str("Search tokens & news"),
                onBarClick = {},
            ),
            feedListCallbacks = FeedListCallbacks(
                onSearchClick = {},
                onMarketOpenClick = {},
                onArticleClick = {},
                onOpenAllNews = {},
                onMarketItemClick = {},
                onSortTypeClick = {},
            ),
            news = NewsUM.Content(articles.filter { it.isTrending.not() }.toImmutableList()),
            trendingArticle = articles.first { it.isTrending },
            marketChartConfig = MarketChartConfig(
                marketCharts = createMarketCharts(marketItems, includeErrorState = false),
                currentSortByType = SortByTypeUM.TopGainers,
            ),
        )
    }

    private fun createMarketCharts(
        items: ImmutableList<MarketsListItemUM>,
        includeErrorState: Boolean,
    ): ImmutableMap<SortByTypeUM, MarketChartUM> {
        val baseCharts = mapOf(
            SortByTypeUM.TopGainers to createChartContent(
                sortByType = SortByTypeUM.TopGainers,
                items = items,
                isSelected = true,
            ),
            SortByTypeUM.Trending to createChartContent(
                sortByType = SortByTypeUM.Trending,
                items = items,
                isSelected = false,
            ),
            SortByTypeUM.ExperiencedBuyers to createChartContent(
                sortByType = SortByTypeUM.ExperiencedBuyers,
                items = items,
                isSelected = false,
            ),
            SortByTypeUM.Staking to MarketChartUM.Loading,
            SortByTypeUM.TopLosers to MarketChartUM.Loading,
        )

        val ratingChart = if (includeErrorState) {
            MarketChartUM.LoadingError(onRetryClicked = {})
        } else {
            createChartContent(
                sortByType = SortByTypeUM.Rating,
                items = items,
                isSelected = false,
            )
        }

        return persistentMapOf(
            SortByTypeUM.Rating to ratingChart,
            *baseCharts.entries.map { it.toPair() }.toTypedArray(),
        )
    }

    private fun createChartContent(
        sortByType: SortByTypeUM,
        items: ImmutableList<MarketsListItemUM>,
        isSelected: Boolean,
    ): MarketChartUM.Content {
        return MarketChartUM.Content(
            items = items,
            sortChartConfig = SortChartConfigUM(
                sortByType = sortByType,
                isSelected = isSelected,
            ),
        )
    }

    private fun createSampleArticles(): ImmutableList<ArticleConfigUM> = persistentListOf(
        ArticleConfigUM(
            id = 1,
            title = "Bitcoin ETF reaches new highs, institutions pile in",
            score = 0.82f,
            createdAt = TextReference.Str("Yesterday"),
            isTrending = true,
            tags = createArticleTags(),
            isViewed = false,
        ),
        ArticleConfigUM(
            id = 2,
            title = "Layer 2 networks battle for dominance amid fee wars",
            score = 0.71f,
            createdAt = TextReference.Str("Yesterday"),
            isTrending = false,
            tags = createArticleTags(),
            isViewed = true,
        ),
        ArticleConfigUM(
            id = 3,
            title = "Stablecoins expand on-ramps across LATAM",
            score = 0.65f,
            createdAt = TextReference.Str("Yesterday"),
            isTrending = false,
            tags = createArticleTags(),
            isViewed = false,
        ),
        ArticleConfigUM(
            id = 4,
            title = "Stablecoins expand on-ramps across LATAM",
            score = 0.65f,
            createdAt = TextReference.Str("Yesterday"),
            isTrending = false,
            tags = createArticleTags(),
            isViewed = false,
        ),
        ArticleConfigUM(
            id = 5,
            title = "Stablecoins expand on-ramps across LATAM",
            score = 0.65f,
            createdAt = TextReference.Str("Yesterday"),
            isTrending = false,
            tags = createArticleTags(),
            isViewed = false,
        ),
        ArticleConfigUM(
            id = 6,
            title = "Stablecoins expand on-ramps across LATAM",
            score = 0.65f,
            createdAt = TextReference.Str("Yesterday"),
            isTrending = false,
            tags = createArticleTags(),
            isViewed = false,
        ),
    )

    private fun createArticleTags(): ImmutableSet<LabelUM> {
        return persistentSetOf(
            LabelUM(
                text = TextReference.Str("BTC"),
                leadingContent = LabelLeadingContentUM.Token(
                    iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/euro-coin.png",
                ),
            ),
            LabelUM(TextReference.Str("Regulation")),
            LabelUM(TextReference.Str("BTC")),
            LabelUM(TextReference.Str("Supply")),
            LabelUM(TextReference.Str("Demand")),
        )
    }

    private fun createSampleMarketItems(): ImmutableList<MarketsListItemUM> = persistentListOf(
        createMarketItem(
            id = "btc",
            name = "Bitcoin",
            symbol = "BTC",
            trendType = PriceChangeType.UP,
            rating = "1",
            marketCap = "$1.2T",
            percent = "12.3%",
        ),
        createMarketItem(
            id = "eth",
            name = "Ethereum",
            symbol = "ETH",
            trendType = PriceChangeType.DOWN,
            rating = "2",
            marketCap = "$480B",
            percent = "-3.1%",
        ),
        createMarketItem(
            id = "sol",
            name = "Solana",
            symbol = "SOL",
            trendType = PriceChangeType.NEUTRAL,
            rating = "7",
            marketCap = "$110B",
            percent = "0.4%",
        ),
        createMarketItem(
            id = "bnb",
            name = "BNB",
            symbol = "BNB",
            trendType = PriceChangeType.NEUTRAL,
            rating = "7",
            marketCap = "$110B",
            percent = "0.4%",
        ),
        createMarketItem(
            id = "doge",
            name = "Dodge",
            symbol = "DOG",
            trendType = PriceChangeType.NEUTRAL,
            rating = "7",
            marketCap = "$110B",
            percent = "0.4%",
        ),
    )

    @Suppress("LongParameterList")
    private fun createMarketItem(
        id: String,
        name: String,
        symbol: String,
        trendType: PriceChangeType,
        rating: String,
        marketCap: String,
        percent: String,
    ): MarketsListItemUM {
        return MarketsListItemUM(
            id = CryptoCurrency.RawID(id),
            name = name,
            currencySymbol = symbol,
            iconUrl = null,
            ratingPosition = rating,
            marketCap = marketCap,
            price = MarketsListItemUM.Price(text = "31 285.72$"),
            trendPercentText = percent,
            trendType = trendType,
            chartData = MarketChartRawData(
                y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
            ),
            isUnder100kMarketCap = false,
            stakingRate = stringReference("APY 12.34%"),
            updateTimestamp = 0,
        )
    }
}