package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadMoreEffect
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardSkeleton
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardSkeletonHeight
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBar
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBarClearance
import com.tangem.features.polymarket.impl.common.ui.PolymarketNextPageLoader
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketCategoryTabUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedScrollPosition
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.math.ceil
import java.math.BigDecimal

// Feed layout: the scrolling header is item 0, the tab band — while loading or with categories — is item 1.
private const val TAB_BAND_INDEX = 1
private const val KEY_HEADER = "header"
private const val KEY_TAB_BAND = "tab_band"
private const val KEY_STATUS = "status"

private val ScrollIdleDebounce = 500.milliseconds

private val ScrollRestoreTimeout = 2.seconds

/** Fixed height of the category tab band: 40dp pills + 8dp vertical padding. */
private val TabBandHeight = 56.dp

private val FeedItemSpacing = 12.dp

private val TabShape = RoundedCornerShape(percent = 50)

// Top rim highlight approximating the glass edge (the real border is a shader in the design).
private val TabRimBrush = Brush.verticalGradient(
    colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
)

@Suppress("LongParameterList")
@Composable
internal fun PolymarketMainScreen(
    state: PolymarketMainUM,
    positions: (@Composable (Modifier) -> Unit)?,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSupportChatClick: () -> Unit,
    onLoadMore: () -> Unit,
    onVisibleEventsChange: (Set<String>) -> Unit,
    onScrollIdle: (index: Int, offset: Int) -> Unit,
    onScrollPositionRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Shared by the inline and the pinned band, so its horizontal scroll survives docking.
    val tabRowState = rememberLazyListState()

    val hasTabBand = state.isCategoriesLoading || state.categories.isNotEmpty()

    val isTabBandPinned by remember(listState, hasTabBand) {
        derivedStateOf { hasTabBand && listState.isTabBandPinned() }
    }

    val selectedCategoryId = state.categories.firstOrNull { it.isSelected }?.id

    PolymarketLoadMoreEffect(listState = listState, onLoadMore = onLoadMore)
    VisibleEventsEffect(listState = listState, onVisibleEventsChange = onVisibleEventsChange)
    ScrollIdleEffect(
        listState = listState,
        selectedCategoryId = selectedCategoryId,
        onScrollIdle = onScrollIdle,
    )
    CategorySwitchEffect(
        listState = listState,
        selectedCategoryId = selectedCategoryId,
        pendingScrollPosition = state.pendingScrollPosition,
        isTabBandPinned = isTabBandPinned,
        onScrollPositionRestore = onScrollPositionRestore,
    )

    ProvideTangemShimmer {
        // The feature lives in its own window: a local haze provider keeps the source and the glass together.
        ProvideHaze {
            Box(modifier = modifier.fillMaxSize()) {
                PolymarketMainScaffold(
                    state = state,
                    positions = positions,
                    listState = listState,
                    tabRowState = tabRowState,
                    isTabBandPinned = isTabBandPinned,
                    onBackClick = onBackClick,
                    onHistoryClick = onHistoryClick,
                    onSupportChatClick = onSupportChatClick,
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
    tabRowState: LazyListState,
    isTabBandPinned: Boolean,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSupportChatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            // The overlay frosts the bar and the pinned tabs together; a second fade would compound the tint.
            TangemTopNavigation(
                title = resourceReference(R.string.prediction_account_title),
                subtitle = resourceReference(R.string.tangempay_multinetwork),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                fadeEnabled = false,
                onBack = onBackClick,
                endButton = {
                    PolymarketMainMenu(onHistoryClick = onHistoryClick, onSupportChatClick = onSupportChatClick)
                },
            )
        },
        overlay = { contentPadding ->
            val topPadding = contentPadding.calculateTopPadding()

            val frostHeight by animateDpAsState(
                targetValue = if (isTabBandPinned) topPadding + TabBandHeight else topPadding,
                label = "headerFrostHeight",
            )
            TangemFade(
                position = TangemFade.Position.Top,
                blur = true,
                modifier = Modifier.height(frostHeight),
            )

            if (isTabBandPinned) {
                CategoryTabBar(
                    categories = state.categories,
                    isLoading = state.isCategoriesLoading,
                    state = tabRowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = topPadding),
                )
            }
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
    tabRowState: LazyListState,
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
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(FeedItemSpacing),
        ) {
            item(key = KEY_HEADER) {
                PolymarketMainHeader(
                    balance = state.balance,
                    isAwaitingFunds = state.isAwaitingFunds,
                    positions = positions,
                )
            }

            if (state.isCategoriesLoading || state.categories.isNotEmpty()) {
                // While pinned the overlay draws the band; the spacer keeps the feed's geometry.
                item(key = KEY_TAB_BAND) {
                    if (isTabBandPinned) {
                        Spacer(modifier = Modifier.height(TabBandHeight))
                    } else {
                        CategoryTabBar(
                            categories = state.categories,
                            isLoading = state.isCategoriesLoading,
                            state = tabRowState,
                        )
                    }
                }
            }

            eventsSection(content = state.content, skeletonCount = skeletonCount)
        }
    }
}

private fun LazyListScope.eventsSection(content: PolymarketMainUM.ContentUM, skeletonCount: Int) {
    when (content) {
        is PolymarketMainUM.ContentUM.Loading -> items(count = skeletonCount, key = { it }) {
            PolymarketEventCardSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
        }
        is PolymarketMainUM.ContentUM.Error -> item(key = KEY_STATUS) {
            PolymarketReloadPrompt(
                text = resourceReference(R.string.prediction_main_events_load_error),
                onReloadClick = content.onReloadClick,
                modifier = Modifier.height(180.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            )
        }
        is PolymarketMainUM.ContentUM.Content -> {
            items(
                items = content.events,
                key = { it.id },
            ) { event ->
                PolymarketEventCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    state = event,
                )
            }

            when (val footer = content.footer) {
                is PolymarketFeedFooterUM.None -> Unit
                is PolymarketFeedFooterUM.Loading -> item(key = KEY_STATUS) {
                    PolymarketNextPageLoader()
                }
                is PolymarketFeedFooterUM.Error -> item(key = KEY_STATUS) {
                    PolymarketReloadPrompt(
                        text = resourceReference(R.string.prediction_main_events_load_error),
                        onReloadClick = footer.onRetryClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun VisibleEventsEffect(listState: LazyListState, onVisibleEventsChange: (Set<String>) -> Unit) {
    val currentOnVisibleEventsChanged by rememberUpdatedState(onVisibleEventsChange)

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { it.key as? String }
                .filterNotTo(mutableSetOf()) { it == KEY_HEADER || it == KEY_TAB_BAND || it == KEY_STATUS }
        }
            .distinctUntilChanged()
            .collect { visibleEventIds -> currentOnVisibleEventsChanged(visibleEventIds) }
    }
}

@Composable
private fun ScrollIdleEffect(listState: LazyListState, selectedCategoryId: Int?, onScrollIdle: (Int, Int) -> Unit) {
    val currentOnScrollIdle by rememberUpdatedState(onScrollIdle)

    // Keyed by the category: a pending report belongs to the tab that is gone.
    LaunchedEffect(listState, selectedCategoryId) {
        snapshotFlow { listState.isScrollInProgress }
            .collectLatest { isScrollInProgress ->
                if (isScrollInProgress) return@collectLatest

                delay(ScrollIdleDebounce)
                currentOnScrollIdle(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
    }
}

/** Waits for the layout before scrolling: an index the list does not hold yet silently clamps to the top. */
@Composable
private fun CategorySwitchEffect(
    listState: LazyListState,
    selectedCategoryId: Int?,
    pendingScrollPosition: PolymarketFeedScrollPosition?,
    isTabBandPinned: Boolean,
    onScrollPositionRestore: () -> Unit,
) {
    val currentOnScrollPositionRestore by rememberUpdatedState(onScrollPositionRestore)
    var appliedCategoryId by remember { mutableStateOf(selectedCategoryId) }

    LaunchedEffect(selectedCategoryId) {
        if (selectedCategoryId == appliedCategoryId) return@LaunchedEffect
        appliedCategoryId = selectedCategoryId

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

@Composable
private fun CategoryTabBar(
    categories: ImmutableList<PolymarketCategoryTabUM>,
    isLoading: Boolean,
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    if (isLoading) {
        PolymarketCategoryTabsSkeleton(modifier = modifier.fillMaxWidth().height(TabBandHeight))
        return
    }
    LazyRow(
        modifier = modifier.height(TabBandHeight),
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = categories, key = { it.id }) { tab ->
            CategoryTab(tab = tab)
        }
    }
}

@Composable
private fun CategoryTab(tab: PolymarketCategoryTabUM) {
    // No backdrop blur on purpose: it would re-blur every frame under the sticky band.
    val background = if (tab.isSelected) {
        Modifier
            .clip(TabShape)
            .background(TangemTheme.colors3.bg.tertiary)
            .border(width = 1.dp, brush = TabRimBrush, shape = TabShape)
    } else {
        Modifier.clip(TabShape)
    }
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .then(background)
            .clickable(onClick = tab.onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tab.label,
            color = if (tab.isSelected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
            maxLines = 1,
        )
    }
}

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
                content = PolymarketMainUM.ContentUM.Content(
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
                content = PolymarketMainUM.ContentUM.Content(
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
                content = PolymarketMainUM.ContentUM.Error(onReloadClick = {}),
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            ),
            positions = null,
            onBackClick = {},
            onSearchClick = {},
            onHistoryClick = {},
            onSupportChatClick = {},
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
                content = PolymarketMainUM.ContentUM.Loading,
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
            onLoadMore = {},
            onVisibleEventsChange = {},
            onScrollIdle = { _, _ -> },
            onScrollPositionRestore = {},
        )
    }
}

private fun previewCategories() = persistentListOf(
    PolymarketCategoryTabUM(id = 1, label = "Trending", isSelected = true, onClick = {}),
    PolymarketCategoryTabUM(id = 2, label = "Business", isSelected = false, onClick = {}),
    PolymarketCategoryTabUM(id = 3, label = "Sport", isSelected = false, onClick = {}),
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