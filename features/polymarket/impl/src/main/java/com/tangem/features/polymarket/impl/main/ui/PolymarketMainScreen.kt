package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadMoreEffect
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBar
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBarClearance
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadingState
import com.tangem.features.polymarket.impl.common.ui.PolymarketNextPageLoader
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketCategoryTabUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

// Feed layout: the scrolling header is item 0, the tab band — when there are categories — is item 1.
private const val TAB_BAND_INDEX = 1
private const val KEY_HEADER = "header"
private const val KEY_TAB_BAND = "tab_band"
private const val KEY_STATUS = "status"

/** Fixed height of the category tab band: 40dp pills + 8dp vertical padding. */
private val TabBandHeight = 56.dp

private val TabShape = RoundedCornerShape(percent = 50)

// Top rim highlight approximating the glass edge (the real border is a shader in the design).
private val TabRimBrush = Brush.verticalGradient(
    colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
)

@Composable
internal fun PolymarketMainScreen(
    state: PolymarketMainUM,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Shared by the inline and the pinned instances of the tab band (only one exists at a time), so the
    // band's horizontal scroll survives docking and undocking.
    val tabRowState = rememberLazyListState()

    val hasCategories = state.categories.isNotEmpty()

    // The band is a real feed item while it travels — so overscroll stretches it together with the content —
    // and docks under the bar once its item reaches the toolbar (or scrolls past the viewport).
    val isTabBandPinned by remember(listState, hasCategories) {
        derivedStateOf { hasCategories && listState.isTabBandPinned() }
    }

    PolymarketLoadMoreEffect(listState = listState, onLoadMore = onLoadMore)

    // The feature lives inside a modal — a separate window whose LocalHazeState belongs to the root
    // window, where haze cannot sample from here. A local provider keeps the source and the glass of
    // this screen in one window.
    ProvideHaze {
        Box(modifier = modifier.fillMaxSize()) {
            PolymarketMainScaffold(
                state = state,
                listState = listState,
                tabRowState = tabRowState,
                isTabBandPinned = isTabBandPinned,
                onBackClick = onBackClick,
            )

            // A sibling of the whole scaffold: the scaffold marks its content as the haze source, and
            // glass inside its own source has nothing to sample.
            PolymarketSearchBar(
                placeholder = stringReference("Search"),
                onClick = onSearchClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun PolymarketMainScaffold(
    state: PolymarketMainUM,
    listState: LazyListState,
    tabRowState: LazyListState,
    isTabBandPinned: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            // The scaffold overlay draws one frost shared by the bar and the pinned tabs, so the bar's own
            // fade is off — two stacked fades would compound their tints.
            TangemTopNavigation(
                title = resourceReference(R.string.prediction_main_title),
                subtitle = resourceReference(R.string.prediction_main_subtitle),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                fadeEnabled = false,
                onBack = onBackClick,
            )
        },
        overlay = { contentPadding ->
            val topPadding = contentPadding.calculateTopPadding()

            // The united header's frost: a single DS fade over the bar alone or, once the band docks,
            // over the bar plus the band — so the blur and tint dissolve to transparent with alpha at
            // the header's bottom edge. The height animates to restretch the fade profile smoothly.
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
                    state = tabRowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = topPadding),
                )
            }
        },
    ) { contentPadding ->
        FeedList(
            modifier = Modifier.fillMaxSize(),
            state = state,
            listState = listState,
            tabRowState = tabRowState,
            isTabBandPinned = isTabBandPinned,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun FeedList(
    state: PolymarketMainUM,
    listState: LazyListState,
    tabRowState: LazyListState,
    isTabBandPinned: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            // The feed scrolls under the floating search bar; the clearance keeps the last card visible.
            bottom = contentPadding.calculateBottomPadding() + PolymarketSearchBarClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Placeholder for the scrolling header (balance, "My Predictions", "Explore Events").
        item(key = KEY_HEADER) {
            HeaderPlaceholder()
        }

        // The tabs outlive the events they filter: they stay put while a category reloads or fails.
        if (state.categories.isNotEmpty()) {
            // While pinned, the scaffold overlay draws the band; the item degrades to an equal-height spacer
            // so the feed keeps its geometry and the inline copy can't catch touches from under the toolbar.
            item(key = KEY_TAB_BAND) {
                if (isTabBandPinned) {
                    Spacer(modifier = Modifier.height(TabBandHeight))
                } else {
                    CategoryTabBar(categories = state.categories, state = tabRowState)
                }
            }
        }

        eventsSection(content = state.content)
    }
}

private fun LazyListScope.eventsSection(content: PolymarketMainUM.ContentUM) {
    when (content) {
        is PolymarketMainUM.ContentUM.Loading -> item(key = KEY_STATUS) {
            PolymarketLoadingState()
        }
        is PolymarketMainUM.ContentUM.Error -> item(key = KEY_STATUS) {
            PolymarketReloadPrompt(
                text = resourceReference(R.string.prediction_main_events_load_error),
                onReloadClick = content.onReloadClick,
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

            if (content.isLoadingNextPage) {
                item(key = KEY_STATUS) {
                    PolymarketNextPageLoader()
                }
            }
        }
    }
}

/**
 * Whether the tab band has reached the toolbar and must dock under it: its feed item's top is at (or past)
 * the pin line, or the item has scrolled out above the viewport entirely.
 *
 * Item offsets are relative to the content area, which starts after the feed's top padding (the measured
 * toolbar height) — so the pin line is simply offset zero.
 */
private fun LazyListState.isTabBandPinned(): Boolean {
    val band = layoutInfo.visibleItemsInfo.firstOrNull { it.key == KEY_TAB_BAND }
    if (band != null) return band.offset <= 0
    val firstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
    return firstVisibleIndex > TAB_BAND_INDEX
}

@Composable
private fun HeaderPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Header placeholder\n(balance, My Predictions, Explore Events)",
            color = TangemTheme.colors3.text.tertiary,
            style = TangemTheme.typography3.body.medium,
        )
    }
}

@Composable
private fun CategoryTabBar(
    categories: ImmutableList<PolymarketCategoryTabUM>,
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
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
    // The selected tab reads as a light "liquid glass" pill: a subtle fill, a drop shadow, and a top rim highlight.
    // The full backdrop-blur (haze) is intentionally deferred — it would re-blur every frame under this sticky bar.
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
                categories = previewCategories(),
                content = PolymarketMainUM.ContentUM.Content(
                    events = previewEvents(),
                    isLoadingNextPage = true,
                ),
            ),
            onBackClick = {},
            onSearchClick = {},
            onLoadMore = {},
        )
    }
}

@Preview(name = "Error Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Error Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketMainScreenErrorPreview() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            PolymarketReloadPrompt(
                text = resourceReference(R.string.prediction_main_events_load_error),
                onReloadClick = {},
            )
        }
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
        volume = null,
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