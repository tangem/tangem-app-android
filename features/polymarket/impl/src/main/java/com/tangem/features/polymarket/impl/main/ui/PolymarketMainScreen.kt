package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.unit.IntOffset
import com.tangem.core.ui.res.TangemTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingTopBar
import com.tangem.core.ui.ds.topbar.collapsing.entity.TangemCollapsingAppBarState
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.PredictionTestTags
import com.tangem.domain.polymarket.model.PolymarketCategoryIcon
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.features.polymarket.impl.common.feed.ui.POLYMARKET_FEED_STATUS_KEY
import com.tangem.features.polymarket.impl.common.feed.ui.PolymarketScrollIdleEffect
import com.tangem.features.polymarket.impl.common.feed.ui.PolymarketVisibleEventsEffect
import com.tangem.features.polymarket.impl.common.feed.ui.polymarketEventsSection
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketCategoryTabUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedContentUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedScrollPosition
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketOutcomeUM
import com.tangem.features.polymarket.impl.common.feed.ui.toTangemTabItemUM
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardSkeletonHeight
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadMoreEffect
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBar
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBarClearance
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import java.math.BigDecimal
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private val ScrollRestoreTimeout = 2.seconds

/** Fixed height of the category tab band: 36dp pills + 8dp vertical padding. */
private val TabBandHeight = 52.dp

private val FilterTabBandHeight = 48.dp

private val TabBandPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

private const val LOADING_TAB_COUNT = 6

private val FeedItemSpacing = 12.dp

@Suppress("LongParameterList")
@Composable
internal fun PolymarketMainScreen(
    state: PolymarketMainUM,
    positions: (@Composable (Modifier) -> Unit)?,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSupportChatClick: () -> Unit,
    onFastTradingSettingsClick: () -> Unit,
    onLoadMore: () -> Unit,
    onVisibleEventsChange: (Set<String>) -> Unit,
    onScrollIdle: (index: Int, offset: Int) -> Unit,
    onScrollPositionRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val headerBehavior = rememberTangemExitUntilCollapsedScrollBehavior(
        isTopOverscrollEnabled = false,
        expandedHeight = 0.dp,
        partialCollapsedHeight = 0.dp,
        snapAnimationSpec = null,
    )

    val tabRowState = rememberScrollState()

    val selectedCategoryId = state.categories.firstOrNull { it.isSelected }?.id
    val filterRowState = key(selectedCategoryId) { rememberScrollState() }

    PolymarketLoadMoreEffect(listState = listState, onLoadMore = onLoadMore)
    PolymarketVisibleEventsEffect(
        listState = listState,
        feedKey = state.selection,
        onVisibleEventsChange = onVisibleEventsChange,
        excludedKeys = setOf(POLYMARKET_FEED_STATUS_KEY),
    )
    PolymarketScrollIdleEffect(
        listState = listState,
        feedKey = state.selection,
        onScrollIdle = onScrollIdle,
    )
    CategorySwitchEffect(
        listState = listState,
        feedKey = state.selection,
        pendingScrollPosition = state.pendingScrollPosition,
        onScrollPositionRestore = onScrollPositionRestore,
    )

    ProvideTangemShimmer {
        // The feature lives in its own window: a local haze provider keeps the source and the glass together.
        ProvideHaze {
            Box(modifier = modifier.fillMaxSize().testTag(PredictionTestTags.MAIN_SCREEN)) {
                PolymarketMainScaffold(
                    state = state,
                    positions = positions,
                    listState = listState,
                    headerBehavior = headerBehavior,
                    tabRowState = tabRowState,
                    filterRowState = filterRowState,
                    onBackClick = onBackClick,
                    onHistoryClick = onHistoryClick,
                    onSupportChatClick = onSupportChatClick,
                    onFastTradingSettingsClick = onFastTradingSettingsClick,
                )

                // Outside the scaffold: glass inside its own haze source has nothing to sample.
                PolymarketSearchBar(
                    placeholder = resourceReference(R.string.common_search),
                    onClick = onSearchClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun PolymarketMainScaffold(
    state: PolymarketMainUM,
    positions: (@Composable (Modifier) -> Unit)?,
    listState: LazyListState,
    headerBehavior: TangemCollapsingAppBarBehavior,
    tabRowState: ScrollState,
    filterRowState: ScrollState,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSupportChatClick: () -> Unit,
    onFastTradingSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasFilters = state.filters.isNotEmpty() || state.onSeeAllClick != null
    val tabBandHeight = TabBandHeight + if (hasFilters) FilterTabBandHeight else 0.dp
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            // The overlay frosts the bar and the pinned tabs together; a second fade would compound the tint.
            TangemTopNavigation(
                title = resourceReference(R.string.prediction_account_title),
                subtitle = resourceReference(R.string.prediction_main_subtitle),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                fadeEnabled = false,
                onBack = onBackClick,
                endButton = {
                    PolymarketMainMenu(
                        onHistoryClick = onHistoryClick,
                        onSupportChatClick = onSupportChatClick,
                        onFastTradingSettingsClick = onFastTradingSettingsClick.takeIf {
                            state.isFastTradingSettingsAvailable
                        },
                    )
                },
            )
        },
        overlay = { contentPadding ->
            val topPadding = contentPadding.calculateTopPadding()
            val topPaddingPx = with(LocalDensity.current) { topPadding.toPx() }
            val hasTabBand = state.isCategoriesLoading || state.categories.isNotEmpty()
            val isTabBandPinned = hasTabBand && headerBehavior.state.heightOffsetLimit < 0f &&
                headerBehavior.state.heightOffset - headerBehavior.state.heightOffsetLimit <= topPaddingPx

            val frostHeight by animateDpAsState(
                targetValue = if (isTabBandPinned) topPadding + tabBandHeight else topPadding,
                label = "headerFrostHeight",
            )
            TangemFade(
                position = TangemFade.Position.Top,
                blur = true,
                modifier = Modifier.height(frostHeight),
            )
            CategoryTabBar(
                categories = state.categories,
                filters = state.filters,
                onSeeAllClick = state.onSeeAllClick,
                filterRowState = filterRowState,
                isLoading = state.isCategoriesLoading,
                state = tabRowState,
                isShown = isTabBandPinned,
                modifier = Modifier.fillMaxWidth().padding(top = topPadding).scrollHeader(headerBehavior.state),
            )
        },
    ) { contentPadding ->
        TangemPullToRefreshSlidingContainer(
            config = state.pullToRefreshConfig,
            indicatorOffset = contentPadding.calculateTopPadding(),
        ) {
            CollapsingFeed(
                state = state,
                positions = positions,
                listState = listState,
                headerBehavior = headerBehavior,
                tabRowState = tabRowState,
                filterRowState = filterRowState,
                contentPadding = contentPadding,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun CollapsingFeed(
    state: PolymarketMainUM,
    positions: (@Composable (Modifier) -> Unit)?,
    listState: LazyListState,
    headerBehavior: TangemCollapsingAppBarBehavior,
    tabRowState: ScrollState,
    filterRowState: ScrollState,
    contentPadding: PaddingValues,
) {
    val headerState = headerBehavior.state
    val visibleHeaderHeight = with(LocalDensity.current) {
        (headerState.heightOffset - headerState.heightOffsetLimit).toDp()
    }
    val topPadding = contentPadding.calculateTopPadding()
    val tabTop = maxOf(visibleHeaderHeight, topPadding)
    val hasTabs = state.isCategoriesLoading || state.categories.isNotEmpty()
    val hasFilters = state.filters.isNotEmpty() || state.onSeeAllClick != null
    val tabHeight = if (hasTabs) TabBandHeight + if (hasFilters) FilterTabBandHeight else 0.dp else 0.dp
    Box(Modifier.fillMaxSize().nestedScroll(headerBehavior.nestedScrollConnection)) {
        TangemCollapsingTopBar(
            state = headerState,
            collapsingPart = {
                PolymarketMainHeader(
                    balance = state.balance,
                    isAwaitingFunds = state.isAwaitingFunds,
                    isRegionRestricted = state.isRegionRestricted,
                    fastTradingBanner = state.fastTradingBanner,
                    positions = positions,
                    modifier = Modifier
                        .onSizeChanged { headerState.updateHeightOffsetLimit(-it.height.toFloat()) }
                        .background(TangemTheme.colors3.bg.primary)
                        .padding(top = topPadding, bottom = FeedItemSpacing)
                        .scrollHeader(headerState),
                )
            },
            body = {
                // Cancel the DS body offset so cards can scroll behind the shared header and frosted bars.
                Box(
                    Modifier.fillMaxSize().offset {
                        IntOffset(0, -(headerState.heightOffset - headerState.heightOffsetLimit).roundToInt())
                    },
                ) {
                    FeedList(
                        content = state.content,
                        listState = listState,
                        contentPadding = PaddingValues(
                            top = tabTop + tabHeight + FeedItemSpacing,
                            bottom = contentPadding.calculateBottomPadding() + PolymarketSearchBarClearance,
                        ),
                    )
                    if (hasTabs) {
                        CategoryTabBar(
                            categories = state.categories,
                            filters = state.filters,
                            onSeeAllClick = state.onSeeAllClick,
                            filterRowState = filterRowState,
                            isLoading = state.isCategoriesLoading,
                            state = tabRowState,
                            isShown = headerState.heightOffsetLimit == 0f || visibleHeaderHeight > topPadding,
                            modifier = Modifier
                                .padding(top = tabTop)
                                .scrollHeader(headerState),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun Modifier.scrollHeader(state: TangemCollapsingAppBarState): Modifier {
    val scope = rememberCoroutineScope()
    return semantics {
        verticalScrollAxisRange = ScrollAxisRange(
            value = { -state.heightOffset },
            maxValue = { -state.heightOffsetLimit },
        )
        scrollBy { _, y ->
            val canScroll = if (y > 0) state.heightOffset > state.heightOffsetLimit else state.heightOffset < 0f
            if (canScroll) scope.launch { state.scroll { scrollBy(-y) } }
            canScroll
        }
    }.scrollable(state, Orientation.Vertical)
}

@Composable
private fun FeedList(
    content: PolymarketFeedContentUM,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val skeletonCount = ceil(maxHeight / (PolymarketEventCardSkeletonHeight + FeedItemSpacing)).toInt()
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(PredictionTestTags.FEED_LIST),
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(FeedItemSpacing),
        ) {
            polymarketEventsSection(content = content, skeletonCount = skeletonCount)
        }
    }
}

/** Waits for the layout before scrolling: an index the list does not hold yet silently clamps to the top. */
@Composable
private fun CategorySwitchEffect(
    listState: LazyListState,
    feedKey: PolymarketEventsListConfig?,
    pendingScrollPosition: PolymarketFeedScrollPosition?,
    onScrollPositionRestore: () -> Unit,
) {
    val currentOnScrollPositionRestore by rememberUpdatedState(onScrollPositionRestore)
    var appliedSelection by remember { mutableStateOf(feedKey) }

    LaunchedEffect(feedKey) {
        if (feedKey == appliedSelection) return@LaunchedEffect
        appliedSelection = feedKey

        val target = pendingScrollPosition ?: PolymarketFeedScrollPosition(index = 0, offset = 0)

        withTimeoutOrNull(ScrollRestoreTimeout) {
            snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > target.index }
        }
        listState.scrollToItem(index = target.index, scrollOffset = target.offset)

        if (pendingScrollPosition != null) currentOnScrollPositionRestore()
    }
}

@Suppress("LongParameterList")
@Composable
private fun CategoryTabBar(
    categories: ImmutableList<PolymarketCategoryTabUM>,
    filters: ImmutableList<TangemTabItemUM.Content>,
    onSeeAllClick: (() -> Unit)?,
    filterRowState: ScrollState,
    isLoading: Boolean,
    state: ScrollState,
    isShown: Boolean,
    modifier: Modifier = Modifier,
) {
    val tabs = remember(categories, isLoading) {
        if (isLoading) loadingTabs() else categories.map { it.toTangemTabItemUM() }.toImmutableList()
    }
    val variant = if (isShown) TangemTabItem.Variant.Material else TangemTabItem.Variant.Transparent
    Column(modifier = modifier.placedOnlyWhen(isShown)) {
        TangemTabNavigation(
            tabs = tabs,
            scrollState = state,
            variant = variant,
            contentPadding = TabBandPadding,
            modifier = Modifier.height(TabBandHeight),
        )
        if (filters.isNotEmpty() || onSeeAllClick != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TangemTabNavigation(
                    tabs = filters,
                    scrollState = filterRowState,
                    variant = variant,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                    modifier = Modifier.weight(1f).height(FilterTabBandHeight),
                )
                onSeeAllClick?.let { onClick ->
                    TangemButton(
                        text = resourceReference(R.string.common_see_all),
                        variant = TangemButton.Variant.Ghost,
                        size = TangemButton.Size.X7,
                        onClick = onClick,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
        }
    }
}

private fun Modifier.placedOnlyWhen(isPlaced: Boolean) = this
    .conditional(!isPlaced) { clearAndSetSemantics {} }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            if (isPlaced) placeable.place(x = 0, y = 0)
        }
    }

private fun loadingTabs(): ImmutableList<TangemTabItemUM> =
    List(LOADING_TAB_COUNT) { index -> TangemTabItemUM.Loading(id = "loading_$index") }.toImmutableList()

@Preview(name = "Content Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Content Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketMainScreenContentPreview() {
    TangemThemePreviewRedesign {
        PolymarketMainScreen(
            state = PolymarketMainUM(
                balance = previewPredictionBalance(BigDecimal("1234.56")),
                categories = previewCategories(),
                content = PolymarketFeedContentUM.Content(
                    events = previewEvents(),
                    footer = PolymarketFeedFooterUM.Loading,
                ),
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            ),
            positions = { modifier ->
                PolymarketPositionsPreview(state = PolymarketPositionsPreviewUM.Loading, modifier = modifier)
            },
            onBackClick = {},
            onSearchClick = {},
            onHistoryClick = {},
            onSupportChatClick = {},
            onFastTradingSettingsClick = {},
            onLoadMore = {},
            onVisibleEventsChange = {},
            onScrollIdle = { _, _ -> },
            onScrollPositionRestore = {},
        )
    }
}

@Preview(name = "Awaiting Funds Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Awaiting Funds Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketMainScreenAwaitingFundsPreview() {
    TangemThemePreviewRedesign {
        PolymarketMainScreen(
            state = PolymarketMainUM(
                balance = previewPredictionBalance(BigDecimal("1234.56")),
                categories = previewCategories(),
                content = PolymarketFeedContentUM.Content(
                    events = previewEvents(),
                    footer = PolymarketFeedFooterUM.Loading,
                ),
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
                isAwaitingFunds = true,
            ),
            positions = { modifier ->
                PolymarketPositionsPreview(state = PolymarketPositionsPreviewUM.Loading, modifier = modifier)
            },
            onBackClick = {},
            onSearchClick = {},
            onHistoryClick = {},
            onSupportChatClick = {},
            onFastTradingSettingsClick = {},
            onLoadMore = {},
            onVisibleEventsChange = {},
            onScrollIdle = { _, _ -> },
            onScrollPositionRestore = {},
        )
    }
}

@Preview(name = "Error Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(
    name = "Error Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Error Compact", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PolymarketMainScreenErrorPreview() {
    TangemThemePreviewRedesign {
        PolymarketMainScreen(
            state = PolymarketMainUM(
                balance = previewPredictionBalance(BigDecimal("1234.56")),
                categories = previewCategories(),
                content = PolymarketFeedContentUM.Error(onReloadClick = {}),
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            ),
            positions = null,
            onBackClick = {},
            onSearchClick = {},
            onHistoryClick = {},
            onSupportChatClick = {},
            onFastTradingSettingsClick = {},
            onLoadMore = {},
            onVisibleEventsChange = {},
            onScrollIdle = { _, _ -> },
            onScrollPositionRestore = {},
        )
    }
}

@Preview(name = "Empty Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(
    name = "Empty Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketMainScreenEmptyPreview() {
    TangemThemePreviewRedesign {
        PolymarketMainScreen(
            state = PolymarketMainUM(
                balance = previewPredictionBalance(BigDecimal("1234.56")),
                categories = previewCategories(),
                content = PolymarketFeedContentUM.Empty,
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            ),
            positions = null,
            onBackClick = {},
            onSearchClick = {},
            onHistoryClick = {},
            onSupportChatClick = {},
            onFastTradingSettingsClick = {},
            onLoadMore = {},
            onVisibleEventsChange = {},
            onScrollIdle = { _, _ -> },
            onScrollPositionRestore = {},
        )
    }
}

@Preview(name = "Loading Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(
    name = "Loading Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Loading Compact", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun PolymarketMainScreenLoadingPreview() {
    TangemThemePreviewRedesign {
        PolymarketMainScreen(
            state = PolymarketMainUM(
                balance = previewPredictionBalance(BigDecimal("1234.56")),
                categories = persistentListOf(),
                isCategoriesLoading = true,
                content = PolymarketFeedContentUM.Loading,
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            ),
            positions = { modifier ->
                PolymarketPositionsPreview(
                    state = PolymarketPositionsPreviewUM.Loading,
                    modifier = modifier,
                    onActiveClick = {},
                    onFinishedClick = {},
                )
            },
            onBackClick = {},
            onSearchClick = {},
            onHistoryClick = {},
            onSupportChatClick = {},
            onFastTradingSettingsClick = {},
            onLoadMore = {},
            onVisibleEventsChange = {},
            onScrollIdle = { _, _ -> },
            onScrollPositionRestore = {},
        )
    }
}

private fun previewCategories() = persistentListOf(
    PolymarketCategoryTabUM(
        id = 1,
        label = stringReference("Trending"),
        icon = PolymarketCategoryIcon.TRENDING,
        isSelected = true,
        onClick = {},
    ),
    PolymarketCategoryTabUM(
        id = 2,
        label = stringReference("Business"),
        icon = null,
        isSelected = false,
        onClick = {},
    ),
    PolymarketCategoryTabUM(
        id = 3,
        label = stringReference("Sport"),
        icon = PolymarketCategoryIcon.SPORTS,
        isSelected = false,
        onClick = {},
    ),
)

private fun previewEvents() = persistentListOf(
    PolymarketEventUM(
        id = "grouped",
        title = stringReference("World Cup winner 2026"),
        iconUrl = null,
        volume = stringReference("Total volume: $6.3M"),
        rows = persistentListOf(
            PolymarketEventRowUM(
                marketId = "france",
                title = stringReference("France"),
                probability = stringReference("24%"),
                outcomes = previewOutcomes(),
            ),
            PolymarketEventRowUM(
                marketId = "uzbekistan",
                title = stringReference("Uzbekistan"),
                probability = stringReference("3%"),
                outcomes = previewOutcomes(),
            ),
        ),
        hiddenMarketsCount = 4,
        onClick = {},
    ),
    PolymarketEventUM(
        id = "plain",
        title = stringReference("Will Ethereum reach $5,000 before the end of the year?"),
        iconUrl = null,
        volume = stringReference("—"),
        rows = persistentListOf(
            PolymarketEventRowUM(
                marketId = "probability",
                title = stringReference("Probability"),
                probability = stringReference("80%"),
                outcomes = previewOutcomes(),
            ),
        ),
        hiddenMarketsCount = 0,
        onClick = {},
    ),
)

private fun previewOutcomes() = persistentListOf(
    PolymarketOutcomeUM(
        assetId = "yes",
        title = stringReference("Yes"),
        onClick = {},
    ),
    PolymarketOutcomeUM(
        assetId = "no",
        title = stringReference("No"),
        onClick = {},
    ),
)