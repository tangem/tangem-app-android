package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketCategoryTabUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

// Feed layout: the scrolling header is item 0, the tab band is item 1.
private const val TAB_BAND_INDEX = 1
private const val KEY_HEADER = "header"
private const val KEY_TAB_BAND = "tab_band"

/** Fixed height of the category tab band: 40dp pills + 8dp vertical padding. */
private val TabBandHeight = 56.dp

private val TabShape = RoundedCornerShape(percent = 50)

// Top rim highlight approximating the glass edge (the real border is a shader in the design).
private val TabRimBrush = Brush.verticalGradient(
    colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
)

@Composable
internal fun PolymarketMainScreen(state: PolymarketMainUM, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    // Shared by the inline and the pinned instances of the tab band (only one exists at a time), so the
    // band's horizontal scroll survives docking and undocking.
    val tabRowState = rememberLazyListState()

    // The band is a real feed item while it travels — so overscroll stretches it together with the content —
    // and docks under the bar once its item reaches the toolbar (or scrolls past the viewport).
    val isTabBandPinned by remember(listState) { derivedStateOf { listState.isTabBandPinned() } }

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
            val content = state.content as? PolymarketMainUM.ContentUM.Content
            if (content != null) {
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
                        categories = content.categories,
                        state = tabRowState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topPadding),
                    )
                }
            }
        },
    ) { contentPadding ->
        when (val content = state.content) {
            PolymarketMainUM.ContentUM.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
            is PolymarketMainUM.ContentUM.Content -> ContentState(
                modifier = Modifier.fillMaxSize(),
                state = content,
                listState = listState,
                tabRowState = tabRowState,
                isTabBandPinned = isTabBandPinned,
                contentPadding = contentPadding,
            )
            PolymarketMainUM.ContentUM.Empty -> MessageState(
                modifier = Modifier.fillMaxSize(),
                text = stringResourceSafe(R.string.prediction_main_empty_events),
            )
            is PolymarketMainUM.ContentUM.Error -> ErrorState(
                modifier = Modifier.fillMaxSize(),
                onRetryClick = content.onRetryClick,
            )
        }
    }
}

@Composable
private fun ContentState(
    state: PolymarketMainUM.ContentUM.Content,
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
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Placeholder for the scrolling header (balance, "My Predictions", "Explore Events").
        item(key = KEY_HEADER) {
            HeaderPlaceholder()
        }

        // While pinned, the scaffold overlay draws the band; the item degrades to an equal-height spacer so
        // the feed keeps its geometry and the inline copy can't catch touches from under the toolbar.
        item(key = KEY_TAB_BAND) {
            if (isTabBandPinned) {
                Spacer(modifier = Modifier.height(TabBandHeight))
            } else {
                CategoryTabBar(categories = state.categories, state = tabRowState)
            }
        }

        items(
            items = state.events,
            key = { it.id },
        ) { event ->
            PolymarketEventCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = event,
            )
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
            .shadow(elevation = 8.dp, shape = TabShape)
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
            // TODO: prepend a 20dp icon (12dp start, 4dp gap) once the BFF fills category.icon.
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

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader(
            color = TangemTheme.colors3.icon.primary,
            size = TangemLoaderSize.X32,
        )
    }
}

@Composable
private fun MessageState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
    }
}

@Composable
private fun ErrorState(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResourceSafe(R.string.common_something_went_wrong),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        TangemButton(
            modifier = Modifier.padding(top = 16.dp),
            size = TangemButton.Size.X10,
            variant = TangemButton.Variant.Primary,
            text = resourceReference(R.string.common_retry),
            onClick = onRetryClick,
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
                accessMode = PolymarketAccessMode.TRADING,
                content = PolymarketMainUM.ContentUM.Content(
                    categories = previewCategories(),
                    events = previewEvents(),
                ),
            ),
            onBackClick = {},
        )
    }
}

@Preview(name = "States Light", showBackground = true, widthDp = 360)
@Preview(
    name = "States Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketMainScreenStatesPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TangemLoader(size = TangemLoaderSize.X24)
            }
            Text(
                text = "No events yet",
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.body.medium,
            )
            ErrorState(onRetryClick = {})
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
    PolymarketEventUM(
        id = "third",
        title = stringReference("Who will win FIFA World Cup 2026 in the USA?"),
        iconUrl = null,
        volume = stringReference("Total volume: $6.3M"),
        rows = persistentListOf(
            PolymarketEventRowUM(
                marketId = "germany",
                title = stringReference("Germany"),
                probability = stringReference("12%"),
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