package com.tangem.features.polymarket.impl.details.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_share_android_20
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketDetailsMarketUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketDetailsOutcomeUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketSubcategoryTabUM
import kotlinx.collections.immutable.persistentListOf

private const val KEY_HEADER = "header"
private const val KEY_SUBCATEGORIES = "subcategories"

/** Gap between the sheet's top edge and the scrolling header at rest. */
private val ContentTopGap = 8.dp

/** Height of the collapsed bar (below the status bar): a 40dp icon + a single-line title, centered. */
private val CollapsedBarHeight = 56.dp

/** End inset of the collapsed title so it clears the two pinned 44dp buttons. */
private val PinnedButtonsClearance = 124.dp

/** Vertical offset centering the pinned 44dp buttons within the collapsed bar. */
private val PinnedButtonsTopOffset = 6.dp

@Composable
internal fun PolymarketEventDetailsScreen(
    state: PolymarketEventDetailsUM,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // The collapsed bar takes over once the scrolling header's bottom passes the bar's bottom edge.
    // Item offsets are relative to the content area, which starts ContentTopGap below the sheet's top
    // edge, so the bar's bottom edge sits at (CollapsedBarHeight - ContentTopGap) in item-offset
    // coordinates.
    val collapseLinePx = with(LocalDensity.current) { (CollapsedBarHeight - ContentTopGap).roundToPx() }
    val isBarCollapsed by remember(listState, collapseLinePx) {
        derivedStateOf { listState.isHeaderScrolledAway(collapseLinePx) }
    }

    TangemTopBarScaffold(
        modifier = modifier,
        // The chrome here is not a bar reserving space: the buttons float over the header's first line,
        // so everything lives in the overlay and the content insets itself below the status bar.
        topBar = {},
        overlay = {
            HeaderOverlay(
                state = state,
                isBarCollapsed = isBarCollapsed,
                onCloseClick = onCloseClick,
            )
        },
    ) { contentPadding ->
        when (state) {
            PolymarketEventDetailsUM.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
            is PolymarketEventDetailsUM.Content -> ContentState(
                modifier = Modifier.fillMaxSize(),
                state = state,
                listState = listState,
                contentPadding = contentPadding,
            )
            is PolymarketEventDetailsUM.Error -> ErrorState(
                modifier = Modifier.fillMaxSize(),
                onRetryClick = state.onRetryClick,
            )
        }
    }
}

/**
 * Whether the scrolling header has moved past the collapse line: its feed item's bottom is above the
 * line, or the item has scrolled out of the viewport entirely.
 */
private fun LazyListState.isHeaderScrolledAway(collapseLinePx: Int): Boolean {
    val header = layoutInfo.visibleItemsInfo.firstOrNull { it.key == KEY_HEADER }
        ?: return layoutInfo.visibleItemsInfo.isNotEmpty()
    return header.offset + header.size <= collapseLinePx
}

@Composable
private fun BoxScope.HeaderOverlay(
    state: PolymarketEventDetailsUM,
    isBarCollapsed: Boolean,
    onCloseClick: () -> Unit,
) {
    val content = state as? PolymarketEventDetailsUM.Content

    // One shared frost for the collapsed bar: the blur and tint dissolve to transparent with alpha at
    // the bar's bottom edge. At rest the header scrolls free of chrome, so the frost shrinks away.
    val frostHeight by animateDpAsState(
        targetValue = if (isBarCollapsed) CollapsedBarHeight else 0.dp,
        label = "detailsFrostHeight",
    )
    TangemFade(
        position = TangemFade.Position.Top,
        blur = true,
        modifier = Modifier.height(frostHeight),
    )

    if (content != null) {
        AnimatedVisibility(
            visible = isBarCollapsed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CollapsedBar(state = content)
        }
    }

    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = PinnedButtonsTopOffset, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (content != null) {
            TangemButton(
                variant = TangemButton.Variant.Material,
                size = TangemButton.Size.X11,
                iconStart = TangemIconUM.Icon(Icons.ic_share_android_20),
                onClick = content.onShareClick,
            )
        }
        TangemButton.Close(onClick = onCloseClick)
    }
}

@Composable
private fun CollapsedBar(state: PolymarketEventDetailsUM.Content, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CollapsedBarHeight)
            .padding(start = 16.dp, end = PinnedButtonsClearance),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EventIcon(iconUrl = state.iconUrl, size = 40.dp)
        Text(
            text = state.title.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.body.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ContentState(
    state: PolymarketEventDetailsUM.Content,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            top = ContentTopGap,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = KEY_HEADER) {
            EventHeader(state = state)
        }

        if (state.subcategories.isNotEmpty()) {
            item(key = KEY_SUBCATEGORIES) {
                SubcategoryBar(subcategories = state.subcategories)
            }
        }

        marketCards(markets = state.activeMarkets)
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

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 780)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketEventDetailsScreenPreview() {
    TangemThemePreviewRedesign {
        PolymarketEventDetailsScreen(
            state = previewContent(),
            onCloseClick = {},
        )
    }
}

private fun previewContent(): PolymarketEventDetailsUM.Content {
    return PolymarketEventDetailsUM.Content(
        title = stringReference("Who will win FIFA World Cup 2026 in the USA?"),
        iconUrl = null,
        totalVolume = stringReference("\$6.3M"),
        change24h = stringReference("2.08%"),
        subcategories = persistentListOf(
            PolymarketSubcategoryTabUM(id = "lines", label = "Game lines", isSelected = true, onClick = {}),
            PolymarketSubcategoryTabUM(id = "score", label = "Exact score", isSelected = false, onClick = {}),
            PolymarketSubcategoryTabUM(id = "halves", label = "Halves", isSelected = false, onClick = {}),
        ),
        activeMarkets = persistentListOf(
            previewMarket(id = "france", title = "France", volume = "\$300K"),
            previewMarket(id = "germany", title = "Germany", volume = "\$250K"),
            previewMarket(id = "argentina", title = "Argentina", volume = "\$6.3M"),
        ),
        onShareClick = {},
    )
}

private fun previewMarket(id: String, title: String, volume: String) = PolymarketDetailsMarketUM(
    id = id,
    title = stringReference(title),
    volume = stringReference(volume),
    iconUrl = null,
    outcomes = persistentListOf(
        PolymarketDetailsOutcomeUM(assetId = "yes", title = stringReference("Yes • 25¢"), onClick = {}),
        PolymarketDetailsOutcomeUM(assetId = "no", title = stringReference("No • 74¢"), onClick = {}),
    ),
)