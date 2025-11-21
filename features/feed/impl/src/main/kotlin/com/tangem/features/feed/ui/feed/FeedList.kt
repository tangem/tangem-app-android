package com.tangem.features.feed.ui.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.common.ui.news.ArticleCard
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.ui.feed.preview.FeedListPreviewDataProvider.createFeedPreviewState
import com.tangem.features.feed.ui.feed.state.FeedListCallbacks
import com.tangem.features.feed.ui.feed.state.FeedListUM
import com.tangem.features.feed.ui.feed.state.MarketChartConfig
import com.tangem.features.feed.ui.feed.state.MarketChartUM
import com.tangem.features.feed.ui.market.components.MarketsListItem
import com.tangem.features.feed.ui.market.components.MarketsListItemPlaceholder
import com.tangem.features.feed.ui.market.state.MarketsListItemUM
import com.tangem.features.feed.ui.market.state.SortByTypeUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun FeedList(state: FeedListUM, onHeaderSizeChange: (Dp) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val background = LocalMainBottomSheetColor.current.value
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .drawBehind { drawRect(background) },
    ) {
        SearchBar(
            modifier = Modifier
                .drawBehind { drawRect(background) }
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                )
                .onGloballyPositioned { coordinates ->
                    if (coordinates.size.height > 0) {
                        with(density) {
                            onHeaderSizeChange(coordinates.size.height.toDp())
                        }
                    }
                }
                .padding(bottom = 4.dp),
            state = state.searchBar,
        )

        SpacerH(20.dp)

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = stringResourceSafe(R.string.feed_market_and_news),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = state.currentDate,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.tertiary,
        )

        SpacerH(32.dp)

        MarketBlock(
            marketChartConfig = state.marketChartConfig,
            feedListCallbacks = state.feedListCallbacks,
        )

        NewsBlock(
            news = state.news,
            feedListCallbacks = state.feedListCallbacks,
            trendingArticle = state.trendingArticle,
        )

        MarketPulseBlock(
            marketChartConfig = state.marketChartConfig,
            feedListCallbacks = state.feedListCallbacks,
        )
    }
}

@Composable
private fun MarketBlock(marketChartConfig: MarketChartConfig, feedListCallbacks: FeedListCallbacks) {
    if (marketChartConfig.marketCharts.isNotEmpty()) {
        Header(
            title = {
                Text(
                    text = stringResourceSafe(R.string.markets_common_title),
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                )
            },
            onSeeAllClick = { feedListCallbacks.onMarketOpenClick(SortByTypeUM.Rating) },
        )

        SpacerH(12.dp)

        marketChartConfig.marketCharts[SortByTypeUM.Rating]?.let { chart ->
            Charts(
                onItemClick = feedListCallbacks.onMarketItemClick,
                modifier = Modifier.padding(horizontal = 16.dp),
                marketChart = chart,
            )
        }
        SpacerH(32.dp)
    }
}

@Composable
private fun MarketPulseBlock(marketChartConfig: MarketChartConfig, feedListCallbacks: FeedListCallbacks) {
    if (marketChartConfig.marketCharts.isNotEmpty()) {
        LazyRow(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            state = rememberLazyListState(),
        ) {
            items(
                items = marketChartConfig.getFilterPreset(),
                key = SortByTypeUM::name,
            ) { sortByTypeUM ->
                FilterChip(
                    sortByTypeUM = sortByTypeUM,
                    isSelected = sortByTypeUM == marketChartConfig.currentSortByType,
                    onClick = { feedListCallbacks.onSortTypeClick(sortByTypeUM) },
                )
            }
        }

        Header(
            title = {
                Text(
                    text = stringResourceSafe(R.string.markets_common_title),
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                )
            },
            onSeeAllClick = { feedListCallbacks.onMarketOpenClick(marketChartConfig.currentSortByType) },
        )

        SpacerH(12.dp)

        AnimatedContent(
            targetState = marketChartConfig.currentSortByType,
            label = "MarketPulseChartAnimation",
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { currentSortType ->
            marketChartConfig.marketCharts[currentSortType]?.let { chart ->
                Charts(
                    onItemClick = feedListCallbacks.onMarketItemClick,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    marketChart = chart,
                )
            }
        }
        SpacerH(32.dp)
    }
}

@Suppress("CanBeNonNullable")
@Composable
private fun NewsBlock(
    feedListCallbacks: FeedListCallbacks,
    news: ImmutableList<ArticleConfigUM>,
    trendingArticle: ArticleConfigUM?,
) {
    if (news.isNotEmpty()) {
        Header(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResourceSafe(R.string.common_news),
                        style = TangemTheme.typography.subtitle1,
                        color = TangemTheme.colors.text.primary1,
                    )

                    SpacerW(4.dp)

                    Image(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_stars_20),
                        contentDescription = null,
                    )

                    SpacerW(2.dp)

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle().copy(
                                    brush = Brush.linearGradient(
                                        GRADIENT_START to LinearGradientFirstPart,
                                        GRADIENT_END to LinearGradientSecondPart,
                                    ),
                                ),
                            ) {
                                append(stringResourceSafe(R.string.feed_tangem_ai))
                            }
                        },
                        style = TangemTheme.typography.subtitle1,
                    )
                }
            },
            onSeeAllClick = feedListCallbacks.onOpenAllNews,
        )

        SpacerH(12.dp)

        trendingArticle?.let { article ->
            ArticleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                articleConfigUM = article,
                onArticleClick = { feedListCallbacks.onArticleClick(article.id) },
            )

            SpacerH(12.dp)
        }

        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            state = rememberLazyListState(),
        ) {
            items(
                items = news,
                key = ArticleConfigUM::id,
            ) { article ->
                ArticleCard(
                    articleConfigUM = article,
                    onArticleClick = { feedListCallbacks.onArticleClick(article.id) },
                    modifier = Modifier.size(164.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(title: @Composable () -> Unit, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        title()

        SecondarySmallButton(
            config = SmallButtonConfig(
                text = TextReference.Res(R.string.common_see_all),
                onClick = onSeeAllClick,
            ),
        )
    }
}

@Composable
private fun Charts(
    marketChart: MarketChartUM,
    onItemClick: (MarketsListItemUM) -> Unit,
    modifier: Modifier = Modifier,
) {
    BlockCard(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            when (marketChart) {
                MarketChartUM.Loading -> {
                    repeat(DEFAULT_CHART_SIZE_IN_MARKET) {
                        MarketsListItemPlaceholder()
                    }
                }
                is MarketChartUM.LoadingError -> {
                    // TODO will be created in [REDACTED_TASK_KEY]
                }
                is MarketChartUM.Content -> {
                    marketChart.items.fastForEach { chart ->
                        MarketsListItem(
                            model = chart,
                            onClick = { onItemClick(chart) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(sortByTypeUM: SortByTypeUM, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(12.dp))
            .background(
                color = if (isSelected) {
                    TangemTheme.colors.button.primary
                } else {
                    TangemTheme.colors.button.secondary
                },
            )
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 8.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = sortByTypeUM.text.resolveReference(),
            style = TangemTheme.typography.button,
            color = if (isSelected) {
                TangemTheme.colors.text.primary2
            } else {
                TangemTheme.colors.text.primary1
            },
        )
    }
}

private const val DEFAULT_CHART_SIZE_IN_MARKET = 5
private const val GRADIENT_START = 0f
private const val GRADIENT_END = 0.5f
private val LinearGradientFirstPart = Color(0xFF635EEC)
private val LinearGradientSecondPart = Color(0xFFE05AED)

@Preview(showBackground = true)
@Composable
private fun FeedListPreview() {
    TangemThemePreview {
        FeedList(
            state = createFeedPreviewState(),
            onHeaderSizeChange = {},
        )
    }
}