package com.tangem.features.feed.ui.search.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.ui.search.SearchContent
import com.tangem.features.feed.ui.search.state.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/** Labeled UI state for [SearchContent] previews. */
internal data class SearchContentPreviewScenario(
    val title: String,
    val content: SearchContentUM,
)

/** Sample market rows, portfolio assets, and hints for [SearchContent] previews (callbacks are no-op). */
@Suppress("MagicNumber", "StringLiteralDuplication", "LargeClass")
internal object SearchContentPreviewFixtures {

    private val chartYSample = persistentListOf(
        0.4, 0.2, 0.45, 0.35, 0.5, 0.55, 0.48, 0.62, 0.58, 0.7, 0.65, 0.8,
    )

    val scenarioInitialEmpty = SearchContentPreviewScenario(
        title = "InitialEmpty – blank screen before input",
        content = SearchContentUM.InitialEmpty,
    )

    val scenarioHistoryEmptyBoth = SearchContentPreviewScenario(
        title = "History – empty hints and recents (no Recents header)",
        content = SearchContentUM.History(
            textHints = persistentListOf(),
            recentTokens = persistentListOf(),
        ),
    )

    val scenarioHistoryHintsOnly = SearchContentPreviewScenario(
        title = "History – text hints only",
        content = SearchContentUM.History(
            textHints = hintsSample(),
            recentTokens = persistentListOf(),
        ),
    )

    val scenarioHistoryRecentsOnly = SearchContentPreviewScenario(
        title = "History – recent tokens only",
        content = SearchContentUM.History(
            textHints = persistentListOf(),
            recentTokens = recentsSample(),
        ),
    )

    val scenarioHistoryFull = SearchContentPreviewScenario(
        title = "History – hints + recents + Clear all",
        content = SearchContentUM.History(
            textHints = hintsSample(),
            recentTokens = recentsSample(),
        ),
    )

    val scenarioResultsMarketEmptyNoPortfolio = SearchContentPreviewScenario(
        title = "Results – Market.Empty, empty portfolio",
        content = SearchContentUM.Results(
            userAssets = persistentListOf(),
            marketTokens = MarketSearchResultUM.Empty,
        ),
    )

    val scenarioResultsMarketEmptyWithPortfolio = SearchContentPreviewScenario(
        title = "Results – Market.Empty, with portfolio",
        content = SearchContentUM.Results(
            userAssets = portfolioTwo(),
            marketTokens = MarketSearchResultUM.Empty,
        ),
    )

    val scenarioResultsLoadingNoPortfolio = SearchContentPreviewScenario(
        title = "Results – Market.Loading, no portfolio",
        content = SearchContentUM.Results(
            userAssets = persistentListOf(),
            marketTokens = MarketSearchResultUM.Loading,
        ),
    )

    val scenarioResultsLoadingWithPortfolio = SearchContentPreviewScenario(
        title = "Results – Market.Loading + portfolio (spacer + Market header)",
        content = SearchContentUM.Results(
            userAssets = portfolioTwo(),
            marketTokens = MarketSearchResultUM.Loading,
        ),
    )

    val scenarioResultsNotFoundNoPortfolio = SearchContentPreviewScenario(
        title = "Results – Market.NotFound, no portfolio",
        content = SearchContentUM.Results(
            userAssets = persistentListOf(),
            marketTokens = MarketSearchResultUM.NotFound,
        ),
    )

    val scenarioResultsNotFoundWithPortfolio = SearchContentPreviewScenario(
        title = "Results – Market.NotFound + portfolio",
        content = SearchContentUM.Results(
            userAssets = portfolioTwo(),
            marketTokens = MarketSearchResultUM.NotFound,
        ),
    )

    val scenarioResultsContentMarketOnly = SearchContentPreviewScenario(
        title = "Results – Market.Content, market only",
        content = SearchContentUM.Results(
            userAssets = persistentListOf(),
            marketTokens = MarketSearchResultUM.Content(
                items = marketListShort(),
                shouldShowUnder100kNotification = false,
                onShowUnder100kClick = {},
            ),
        ),
    )

    val scenarioResultsContentPortfolioAndMarket = SearchContentPreviewScenario(
        title = "Results – portfolio + market (no under 100k)",
        content = SearchContentUM.Results(
            userAssets = portfolioTwo(),
            marketTokens = MarketSearchResultUM.Content(
                items = marketListShort(),
                shouldShowUnder100kNotification = false,
                onShowUnder100kClick = {},
            ),
        ),
    )

    val scenarioResultsContentWithUnder100kBanner = SearchContentPreviewScenario(
        title = "Results – portfolio + market + under 100k banner",
        content = SearchContentUM.Results(
            userAssets = portfolioTwo(),
            marketTokens = MarketSearchResultUM.Content(
                items = marketListShort(),
                shouldShowUnder100kNotification = true,
                onShowUnder100kClick = {},
            ),
        ),
    )

    val scenarioResultsLongMarketScroll = SearchContentPreviewScenario(
        title = "Results – long market list (scroll)",
        content = SearchContentUM.Results(
            userAssets = portfolioTwo(),
            marketTokens = MarketSearchResultUM.Content(
                items = marketListLong(),
                shouldShowUnder100kNotification = true,
                onShowUnder100kClick = {},
            ),
        ),
    )

    private fun marketToken(
        rawId: String,
        name: String,
        symbol: String,
        trend: PriceChangeType = PriceChangeType.UP,
        priceText: String = "$98,765.43",
        trendText: String = "+2.34%",
        marketCap: String? = "$1.2 T",
        rating: String? = "1",
        chart: MarketChartRawData? = MarketChartRawData(y = chartYSample),
        staking: Boolean = false,
        updateTimestamp: Long = rawId.hashCode().toLong(),
    ): MarketsListItemUM = MarketsListItemUM(
        id = CryptoCurrency.RawID(rawId),
        name = name,
        currencySymbol = symbol,
        iconUrl = null,
        ratingPosition = rating,
        marketCap = marketCap,
        price = MarketsListItemUM.Price(
            text = priceText,
            annotated = stringReference(priceText),
            fiatPrice = BigDecimal(123123),
        ),
        trendPercentText = trendText,
        trendType = trend,
        chartData = chart,
        isUnder100kMarketCap = false,
        stakingRate = if (staking) stringReference("APY 4.2%") else null,
        updateTimestamp = updateTimestamp,
    )

    private fun userAsset(
        id: String,
        name: String,
        symbol: String,
        accountName: String,
        iconUrl: String? = null,
    ): UserAssetItemUM = UserAssetItemUM(
        id = id,
        tokenIconUrl = iconUrl,
        tokenName = name,
        tokenSymbol = symbol,
        accountName = accountName,
        onClick = {},
    )

    private fun textHint(text: String): TextHintItemUM = TextHintItemUM(text = text)

    private fun hintsSample(): ImmutableList<TextHintItemUM> = persistentListOf(
        textHint("bitcoin"),
        textHint("sol"),
        textHint("very long search query example for ellipsis"),
    )

    private fun recentsSample(): ImmutableList<MarketsListItemUM> = persistentListOf(
        marketToken(rawId = "btc_r", name = "Bitcoin", symbol = "BTC"),
        marketToken(
            rawId = "eth_r",
            name = "Ethereum",
            symbol = "ETH",
            trend = PriceChangeType.DOWN,
            trendText = "−1.02%",
            rating = "2",
        ),
        marketToken(
            rawId = "long_r",
            name = "A Very Long Token Name That Should Ellipsize In The List",
            symbol = "LONG",
            trend = PriceChangeType.NEUTRAL,
            trendText = "0.00%",
            marketCap = "$999.123456789 B",
            rating = "42",
        ),
    )

    private fun portfolioTwo(): ImmutableList<UserAssetItemUM> = persistentListOf(
        userAsset(id = "p1", name = "Ethereum", symbol = "ETH", accountName = "Main wallet"),
        userAsset(
            id = "p2",
            name = "Polygon",
            symbol = "POL",
            accountName = "Account with a long label for preview",
        ),
    )

    private fun marketListShort(): ImmutableList<MarketsListItemUM> = persistentListOf(
        marketToken(rawId = "m1", name = "Bitcoin", symbol = "BTC", rating = "1"),
        marketToken(
            rawId = "m2",
            name = "Ethereum",
            symbol = "ETH",
            trend = PriceChangeType.DOWN,
            trendText = "−0.55%",
            rating = "2",
        ),
        marketToken(
            rawId = "m3",
            name = "Solana",
            symbol = "SOL",
            trend = PriceChangeType.NEUTRAL,
            trendText = "0.12%",
            rating = "3",
        ),
    )

    @Suppress("LongMethod")
    private fun marketListLong(): ImmutableList<MarketsListItemUM> = listOf(
        marketToken(
            rawId = "L1",
            name = "Arbitrum",
            symbol = "ARB",
            trend = PriceChangeType.UP,
            priceText = "$1.12",
            trendText = "+8.1%",
            marketCap = "$3.1 B",
            rating = "10",
        ),
        marketToken(
            rawId = "L2",
            name = "Optimism",
            symbol = "OP",
            trend = PriceChangeType.DOWN,
            priceText = "$2.34",
            trendText = "−3.2%",
            marketCap = "$2.8 B",
            rating = "11",
        ),
        marketToken(
            rawId = "L3",
            name = "Base",
            symbol = "—",
            trend = PriceChangeType.NEUTRAL,
            priceText = "$0.98",
            trendText = "0.0%",
            marketCap = "$1.9 B",
            rating = "12",
            chart = null,
        ),
        marketToken(
            rawId = "L4",
            name = "Avalanche",
            symbol = "AVAX",
            trend = PriceChangeType.UP,
            priceText = "$36.5",
            trendText = "+4.4%",
            marketCap = "$14 B",
            rating = "13",
            staking = true,
        ),
        marketToken(
            rawId = "L5",
            name = "Polkadot",
            symbol = "DOT",
            trend = PriceChangeType.DOWN,
            priceText = "$6.12",
            trendText = "−2.1%",
            marketCap = "$8 B",
            rating = "14",
        ),
        marketToken(
            rawId = "L6",
            name = "Cosmos",
            symbol = "ATOM",
            trend = PriceChangeType.UP,
            priceText = "$8.90",
            trendText = "+1.1%",
            marketCap = "$3.4 B",
            rating = "15",
        ),
        marketToken(
            rawId = "L7",
            name = "Near",
            symbol = "NEAR",
            trend = PriceChangeType.DOWN,
            priceText = "$4.56",
            trendText = "−0.8%",
            marketCap = "$4.5 B",
            rating = "16",
        ),
        marketToken(
            rawId = "L8",
            name = "Sui",
            symbol = "SUI",
            trend = PriceChangeType.UP,
            priceText = "$2.10",
            trendText = "+12.3%",
            marketCap = "$2.2 B",
            rating = "17",
        ),
    ).toImmutableList()

    fun allScenarios(): List<SearchContentPreviewScenario> = listOf(
        scenarioInitialEmpty,
        scenarioHistoryEmptyBoth,
        scenarioHistoryHintsOnly,
        scenarioHistoryRecentsOnly,
        scenarioHistoryFull,
        scenarioResultsMarketEmptyNoPortfolio,
        scenarioResultsMarketEmptyWithPortfolio,
        scenarioResultsLoadingNoPortfolio,
        scenarioResultsLoadingWithPortfolio,
        scenarioResultsNotFoundNoPortfolio,
        scenarioResultsNotFoundWithPortfolio,
        scenarioResultsContentMarketOnly,
        scenarioResultsContentPortfolioAndMarket,
        scenarioResultsContentWithUnder100kBanner,
        scenarioResultsLongMarketScroll,
    )
}

private val SearchContentPreviewCallbacks = SearchCallbacks(
    onLoadMore = {},
    onClearHintsClick = {},
    onTextHintClick = { _ -> },
    onResultMarketTokenClick = { _ -> },
)

/** All [SearchContentPreviewScenario] values for the Preview Parameter dropdown in Android Studio. */
internal class SearchContentPreviewParameterProvider : PreviewParameterProvider<SearchContentPreviewScenario> {
    override val values: Sequence<SearchContentPreviewScenario>
        get() = SearchContentPreviewFixtures.allScenarios().asSequence()
}

@Composable
private fun SearchContentPreviewHost(
    scenario: SearchContentPreviewScenario,
    modifier: Modifier = Modifier,
    previewHeightDp: Int = 720,
) {
    Box(
        modifier = modifier
            .height(previewHeightDp.dp)
            .fillMaxWidth()
            .background(TangemTheme.colors.background.primary),
    ) {
        SearchContent(
            content = scenario.content,
            searchCallbacks = SearchContentPreviewCallbacks,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(),
        )
    }
}

// region Named previews (quick access without cycling parameters)

@Composable
@Preview(name = "01 Initial empty", showBackground = true, widthDp = 360, heightDp = 720)
@Preview(
    name = "01 Initial empty (night)",
    showBackground = true,
    widthDp = 360,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun SearchContentPreview_InitialEmpty() {
    TangemThemePreviewRedesign {
        SearchContentPreviewHost(scenario = SearchContentPreviewFixtures.scenarioInitialEmpty)
    }
}

@Composable
@Preview(name = "02 History full", showBackground = true, widthDp = 360, heightDp = 720)
@Preview(
    name = "02 History full (night)",
    showBackground = true,
    widthDp = 360,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun SearchContentPreview_HistoryFull() {
    TangemThemePreviewRedesign {
        SearchContentPreviewHost(scenario = SearchContentPreviewFixtures.scenarioHistoryFull)
    }
}

@Composable
@Preview(name = "03 Results loading + portfolio", showBackground = true, widthDp = 360, heightDp = 720)
@Preview(
    name = "03 Results loading + portfolio (night)",
    showBackground = true,
    widthDp = 360,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun SearchContentPreview_ResultsLoadingWithPortfolio() {
    TangemThemePreviewRedesign {
        SearchContentPreviewHost(scenario = SearchContentPreviewFixtures.scenarioResultsLoadingWithPortfolio)
    }
}

@Composable
@Preview(name = "04 Results not found", showBackground = true, widthDp = 360, heightDp = 720)
@Preview(
    name = "04 Results not found (night)",
    showBackground = true,
    widthDp = 360,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun SearchContentPreview_ResultsNotFound() {
    TangemThemePreviewRedesign {
        SearchContentPreviewHost(scenario = SearchContentPreviewFixtures.scenarioResultsNotFoundNoPortfolio)
    }
}

@Composable
@Preview(name = "05 Results market + under 100k", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(
    name = "05 Results market + under 100k (night)",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun SearchContentPreview_ResultsWithUnder100kBanner() {
    TangemThemePreviewRedesign {
        SearchContentPreviewHost(
            scenario = SearchContentPreviewFixtures.scenarioResultsContentWithUnder100kBanner,
            previewHeightDp = 800,
        )
    }
}

@Composable
@Preview(name = "06 Results long scroll", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(
    name = "06 Results long scroll (night)",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun SearchContentPreview_ResultsLongList() {
    TangemThemePreviewRedesign {
        SearchContentPreviewHost(
            scenario = SearchContentPreviewFixtures.scenarioResultsLongMarketScroll,
            previewHeightDp = 640,
        )
    }
}

// endregion

/** All scenarios via Preview Parameter; scenario label is the [SearchContentPreviewScenario] title property. */
@Composable
@Preview(name = "All scenarios (parameter)", showBackground = true, widthDp = 360, heightDp = 720)
@Preview(
    name = "All scenarios (parameter, night)",
    showBackground = true,
    widthDp = 360,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun SearchContentPreview_AllScenarios(
    @PreviewParameter(SearchContentPreviewParameterProvider::class) scenario: SearchContentPreviewScenario,
) {
    TangemThemePreviewRedesign {
        SearchContentPreviewHost(
            scenario = scenario,
            previewHeightDp = when (scenario.title) {
                SearchContentPreviewFixtures.scenarioResultsLongMarketScroll.title -> 640
                SearchContentPreviewFixtures.scenarioResultsContentWithUnder100kBanner.title -> 800
                else -> 720
            },
        )
    }
}