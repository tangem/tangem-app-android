package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.PredictionTestTags
import com.tangem.domain.polymarket.model.PolymarketCategoryIcon
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadMoreEffect
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardSkeletonHeight
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBar
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBarClearance
import com.tangem.features.polymarket.impl.common.feed.ui.PolymarketScrollIdleEffect
import com.tangem.features.polymarket.impl.common.feed.ui.toTangemTabItemUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketCategoryTabUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedScrollPosition
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketOutcomeUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.math.ceil
import java.math.BigDecimal
import com.tangem.features.polymarket.impl.common.feed.ui.POLYMARKET_FEED_STATUS_KEY
import com.tangem.features.polymarket.impl.common.feed.ui.PolymarketVisibleEventsEffect
import com.tangem.features.polymarket.impl.common.feed.ui.polymarketEventsSection
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedContentUM

// Feed layout: the scrolling header is item 0, the tab band — while loading or with categories — is item 1.
private const val TAB_BAND_INDEX = 1
private const val KEY_HEADER = "header"
private const val KEY_TAB_BAND = "tab_band"

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

    // Shared by the inline and the pinned band, so its horizontal scroll survives docking.
    val tabRowState = rememberScrollState()

    val hasTabBand = state.isCategoriesLoading || state.categories.isNotEmpty()

    val isTabBandPinned by remember(listState, hasTabBand) {
        derivedStateOf { hasTabBand && listState.isTabBandPinned() }
    }

    val selectedCategoryId = state.categories.firstOrNull { it.isSelected }?.id
    val filterRowState = key(selectedCategoryId) { rememberScrollState() }

    PolymarketLoadMoreEffect(listState = listState, onLoadMore = onLoadMore)
    PolymarketVisibleEventsEffect(
        listState = listState,
        feedKey = state.selection,
        onVisibleEventsChange = onVisibleEventsChange,
        excludedKeys = setOf(KEY_HEADER, KEY_TAB_BAND, POLYMARKET_FEED_STATUS_KEY),
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
        isTabBandPinned = isTabBandPinned,
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
                    tabRowState = tabRowState,
                    filterRowState = filterRowState,
                    isTabBandPinned = isTabBandPinned,
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
    tabRowState: ScrollState,
    filterRowState: ScrollState,
    isTabBandPinned: Boolean,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPadding),
            )
        },
    ) { contentPadding ->
        TangemPullToRefreshSlidingContainer(
            config = state.pullToRefreshConfig,
            indicatorOffset = contentPadding.calculateTopPadding(),
        ) {
            FeedList(
                modifier = Modifier.fillMaxSize(),
                state = state,
                positions = positions,
                listState = listState,
                tabRowState = tabRowState,
                filterRowState = filterRowState,
                isTabBandPinned = isTabBandPinned,
                contentPadding = contentPadding,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun FeedList(
    state: PolymarketMainUM,
    positions: (@Composable (Modifier) -> Unit)?,
    listState: LazyListState,
    tabRowState: ScrollState,
    filterRowState: ScrollState,
    isTabBandPinned: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val topPadding = contentPadding.calculateTopPadding()
    // The feed scrolls under the floating search bar; the clearance keeps the last card visible.
    val bottomPadding = contentPadding.calculateBottomPadding() + PolymarketSearchBarClearance

    BoxWithConstraints(modifier = modifier) {
        val skeletonCount = ceil(maxHeight / (PolymarketEventCardSkeletonHeight + FeedItemSpacing)).toInt()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(PredictionTestTags.FEED_LIST),
            state = listState,
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(FeedItemSpacing),
        ) {
            item(key = KEY_HEADER) {
                PolymarketMainHeader(
                    balance = state.balance,
                    isAwaitingFunds = state.isAwaitingFunds,
                    isRegionRestricted = state.isRegionRestricted,
                    positions = positions,
                )
            }

            if (state.isCategoriesLoading || state.categories.isNotEmpty()) {
                // While pinned the overlay draws the band; the unplaced inline band keeps the feed's geometry.
                item(key = KEY_TAB_BAND) {
                    KeepItemComposed()
                    CategoryTabBar(
                        categories = state.categories,
                        filters = state.filters,
                        onSeeAllClick = state.onSeeAllClick,
                        filterRowState = filterRowState,
                        isLoading = state.isCategoriesLoading,
                        state = tabRowState,
                        isShown = !isTabBandPinned,
                    )
                }
            }

            polymarketEventsSection(content = state.content, skeletonCount = skeletonCount)
        }
    }
}

/** Waits for the layout before scrolling: an index the list does not hold yet silently clamps to the top. */
@Composable
private fun CategorySwitchEffect(
    listState: LazyListState,
    feedKey: PolymarketEventsListConfig?,
    pendingScrollPosition: PolymarketFeedScrollPosition?,
    isTabBandPinned: Boolean,
    onScrollPositionRestore: () -> Unit,
) {
    val currentOnScrollPositionRestore by rememberUpdatedState(onScrollPositionRestore)
    var appliedSelection by remember { mutableStateOf(feedKey) }

    LaunchedEffect(feedKey) {
        if (feedKey == appliedSelection) return@LaunchedEffect
        appliedSelection = feedKey

        val target = pendingScrollPosition
            ?: PolymarketFeedScrollPosition(index = TAB_BAND_INDEX, offset = 0).takeIf { isTabBandPinned }
            ?: return@LaunchedEffect

        withTimeoutOrNull(ScrollRestoreTimeout) {
            snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > target.index }
        }
        listState.scrollToItem(index = target.index, scrollOffset = target.offset)

        if (pendingScrollPosition != null) currentOnScrollPositionRestore()
    }
}

/** Item offsets are relative to the content area (after the top padding), so the pin line is offset zero. */
private fun LazyListState.isTabBandPinned(): Boolean {
    val band = layoutInfo.visibleItemsInfo.firstOrNull { it.key == KEY_TAB_BAND }
    if (band != null) return band.offset <= 0
    val firstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
    return firstVisibleIndex > TAB_BAND_INDEX
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

@Composable
private fun KeepItemComposed() {
    val pinnableContainer = LocalPinnableContainer.current
    DisposableEffect(pinnableContainer) {
        val pinnedHandle = pinnableContainer?.pin()
        onDispose { pinnedHandle?.release() }
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