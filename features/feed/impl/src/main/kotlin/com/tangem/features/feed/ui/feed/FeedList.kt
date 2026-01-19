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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.markets.MarketsListItem
import com.tangem.common.ui.markets.MarketsListItemPlaceholder
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.common.ui.news.ArticleCard
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.common.ui.news.ShowMoreArticlesCard
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.feed.preview.FeedListPreviewDataProvider.createFeedPreviewState
import com.tangem.features.feed.ui.feed.state.*

@Composable
internal fun FeedListHeader(
    isSearchBarClickable: Boolean,
    feedListSearchBar: FeedListSearchBar,
    modifier: Modifier = Modifier,
) {
    val background = LocalMainBottomSheetColor.current.value
    FeedSearchBar(
        isSearchBarClickable = isSearchBarClickable,
        feedListSearchBar = feedListSearchBar,
        modifier = modifier
            .drawBehind { drawRect(background) }
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
    )
}

@Composable
internal fun FeedList(state: FeedListUM, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    AnimatedContent(
        modifier = modifier,
        targetState = state.globalState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { animatedState ->
        when (animatedState) {
            is GlobalFeedState.Loading -> {
                FeedListLoading(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .drawBehind { drawRect(background) },
                )
            }
            is GlobalFeedState.Error -> {
                FeedListGlobalError(
                    onRetryClick = animatedState.onRetryClicked,
                    modifier = Modifier.drawBehind { drawRect(background) },
                    currentDate = state.currentDate,
                )
            }
            is GlobalFeedState.Content -> {
                FeedListContent(
                    modifier = Modifier,
                    state = state,
                )
            }
        }
    }
}

@Composable
private fun FeedSearchBar(
    isSearchBarClickable: Boolean,
    feedListSearchBar: FeedListSearchBar,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(color = TangemTheme.colors.field.focused)
            .conditional(condition = isSearchBarClickable) {
                clickable(onClick = feedListSearchBar.onBarClick)
            }
            .padding(14.dp),
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size20),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_search_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )

        SpacerW(14.dp)

        Text(
            text = feedListSearchBar.placeholderText.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun FeedListContent(state: FeedListUM, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            .drawBehind { drawRect(background) },
    ) {
        DateBlock(state.currentDate)
        SpacerH(32.dp)

        MarketBlock(
            marketChart = state.marketChartConfig.marketCharts[SortByTypeUM.Rating],
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
private fun MarketBlock(marketChart: MarketChartUM?, feedListCallbacks: FeedListCallbacks) {
    AnimatedContent(
        targetState = marketChart,
        label = "MarketBlockChartAnimation",
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { currentChart ->
        when (currentChart) {
            is MarketChartUM.Content,
            MarketChartUM.Loading,
            is MarketChartUM.LoadingError,
            -> {
                Column(modifier = Modifier.fillMaxWidth()) {
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

                    Charts(
                        onItemClick = feedListCallbacks.onMarketItemClick,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        marketChart = currentChart,
                    )

                    SpacerH(32.dp)
                }
            }
            null -> Unit
        }
    }
}

@Composable
private fun MarketPulseBlock(marketChartConfig: MarketChartConfig, feedListCallbacks: FeedListCallbacks) {
    val onSeeAllClick by rememberUpdatedState {
        feedListCallbacks.onMarketOpenClick(marketChartConfig.currentSortByType)
    }
    if (marketChartConfig.marketCharts.isNotEmpty()) {
        Header(
            title = {
                Text(
                    text = stringResourceSafe(R.string.markets_pulse_common_title),
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                )
            },
            onSeeAllClick = { onSeeAllClick() },
        )

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
private fun NewsBlock(feedListCallbacks: FeedListCallbacks, news: NewsUM, trendingArticle: ArticleConfigUM?) {
    AnimatedContent(news.newsUMState) { newsUMState ->
        when (newsUMState) {
            NewsUMState.LOADING -> NewsLoadingBlock()
            NewsUMState.CONTENT -> {
                if (news.content.isNotEmpty()) {
                    NewsContentBlock(
                        feedListCallbacks = feedListCallbacks,
                        news = news,
                        trendingArticle = trendingArticle,
                    )
                }
            }
            NewsUMState.ERROR -> NewsErrorBlock(onRetryClick = news.onRetryClicked)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun NewsContentBlock(feedListCallbacks: FeedListCallbacks, news: NewsUM, trendingArticle: ArticleConfigUM?) {
    val listState = rememberLazyListState()
    val articlesReadStatus = remember(news.content) {
        news.content.map { it.isViewed }
    }
    LaunchedEffect(articlesReadStatus) {
        listState.requestScrollToItem(0)
    }
    Column {
        Header(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResourceSafe(R.string.common_news),
                        style = TangemTheme.typography.h3,
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
            onSeeAllClick = { feedListCallbacks.onOpenAllNews(false) },
        )
        SpacerH(12.dp)

        if (trendingArticle != null) {
            Column {
                ArticleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    articleConfigUM = trendingArticle,
                    onArticleClick = { feedListCallbacks.onArticleClick(trendingArticle.id) },
                    colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
                )
                SpacerH(12.dp)
            }
        }

        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            state = listState,
        ) {
            itemsIndexed(
                items = news.content,
                key = { _, article -> article.id },
                contentType = { _, _ -> "article" },
            ) { index, article ->
                val articleModifier = if (index == FOURTH_ITEM_INDEX) {
                    Modifier.onFirstVisible(
                        minFractionVisible = 0.5f,
                        callback = feedListCallbacks.onSliderScroll,
                    )
                } else {
                    Modifier
                }
                ArticleCard(
                    articleConfigUM = article,
                    onArticleClick = { feedListCallbacks.onArticleClick(article.id) },
                    modifier = articleModifier
                        .heightIn(min = 164.dp)
                        .width(216.dp),
                    colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
                )
            }

            item(contentType = "show_more") {
                ShowMoreArticlesCard(
                    modifier = Modifier
                        .width(216.dp)
                        .heightIn(min = 164.dp)
                        .onFirstVisible(
                            minFractionVisible = 0.5f,
                            callback = feedListCallbacks.onSliderEndReached,
                        ),
                    onClick = { feedListCallbacks.onOpenAllNews(true) },
                )
            }
        }
        SpacerH(32.dp)
    }
}

@Composable
private fun Header(title: @Composable () -> Unit, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            when (marketChart) {
                MarketChartUM.Loading -> {
                    repeat(DEFAULT_CHART_SIZE_IN_MARKET) {
                        MarketsListItemPlaceholder()
                    }
                }
                is MarketChartUM.LoadingError -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 35.dp, horizontal = 10.dp),
                    ) {
                        UnableToLoadData(
                            onRetryClick = marketChart.onRetryClicked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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

@Composable
private fun FeedListGlobalError(onRetryClick: () -> Unit, currentDate: String, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    Column(modifier) {
        DateBlock(currentDate)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(background) }
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            UnableToLoadData(onRetryClick = onRetryClick)
        }
    }
}

@Composable
private fun NewsErrorBlock(onRetryClick: () -> Unit) {
    Column {
        Header(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResourceSafe(R.string.common_news),
                        style = TangemTheme.typography.h3,
                        color = TangemTheme.colors.text.primary1,
                    )
                }
            },
            onSeeAllClick = {},
        )
        SpacerH(12.dp)
        BlockCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
        ) {
            UnableToLoadData(
                onRetryClick = onRetryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 35.dp, horizontal = 10.dp),
            )
        }
        SpacerH(32.dp)
    }
}

@Composable
private fun ColumnScope.DateBlock(currentDate: String) {
    SpacerH(20.dp)
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        text = stringResourceSafe(R.string.feed_market_and_news),
        style = TangemTheme.typography.h2,
        color = TangemTheme.colors.text.primary1,
    )
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        text = currentDate,
        style = TangemTheme.typography.h2,
        color = TangemTheme.colors.text.tertiary,
    )
}

private const val DEFAULT_CHART_SIZE_IN_MARKET = 5
private const val FOURTH_ITEM_INDEX = 3
private const val GRADIENT_START = 0f
private const val GRADIENT_END = 0.5f
private val LinearGradientFirstPart = Color(0xFF635EEC)
private val LinearGradientSecondPart = Color(0xFFE05AED)

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun FeedListPreview() {
    TangemThemePreview {
        FeedList(state = createFeedPreviewState())
    }
}