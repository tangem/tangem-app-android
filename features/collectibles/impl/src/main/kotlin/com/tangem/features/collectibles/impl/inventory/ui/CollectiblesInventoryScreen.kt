package com.tangem.features.collectibles.impl.inventory.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.containers.pullToRefresh.getPullToRefreshIndicatorOffset
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.discovery.ui.CollectiblesTabBand
import com.tangem.features.collectibles.impl.inventory.ui.preview.CollectiblesInventoryPreviewData
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesSearchFooter
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CollectiblesInventoryScreen(
    state: CollectiblesInventoryUM,
    pullToRefresh: PullToRefreshConfig,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var footerHeight by remember { mutableIntStateOf(0) }
    val gridState = key(state.contentKey) { rememberLazyGridState() }
    val tabScrollState = rememberScrollState()
    val isContentUnderTabs by remember(gridState) {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0 }
    }
    val pullToRefreshState = rememberPullToRefreshState()
    val pullOffset = getPullToRefreshIndicatorOffset(
        pullToRefreshConfig = pullToRefresh,
        pullToRefreshState = pullToRefreshState,
    )

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                title = resourceReference(R.string.collectibles_inventory_title),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                fadeEnabled = false,
                onClose = onCloseClick,
            )
        },
        overlay = { contentPadding ->
            InventoryTabsHeader(
                tabs = state.tabs,
                tabScrollState = tabScrollState,
                topPadding = contentPadding.calculateTopPadding(),
                pullOffset = pullOffset,
                isContentUnderTabs = isContentUnderTabs,
            )

            CollectiblesSearchFooter(
                state = state.search,
                focusRequester = focusRequester,
                isShown = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeight = size.height },
            )
        },
    ) { contentPadding ->
        val bottomInset = with(LocalDensity.current) { footerHeight.toDp() } + 12.dp

        TangemPullToRefreshSlidingContainer(
            config = pullToRefresh,
            state = pullToRefreshState,
            indicatorOffset = contentPadding.calculateTopPadding(),
        ) {
            CollectiblesInventoryContent(
                content = state.content,
                gridState = gridState,
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 52.dp,
                    bottom = bottomInset,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * @param pullOffset how far a pull-to-refresh has slid the content down; the tabs follow it
 */
@Composable
private fun InventoryTabsHeader(
    tabs: ImmutableList<TangemTabItemUM>,
    tabScrollState: ScrollState,
    topPadding: Dp,
    pullOffset: Dp,
    isContentUnderTabs: Boolean,
) {
    val frostAlpha by animateFloatAsState(
        targetValue = if (isContentUnderTabs) 1f else 0f,
        label = "tabsFrostAlpha",
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        // A haze source above the list, so the top bar's material buttons sample the frost and not only
        // the content under it; the nested fade still blurs only the list (lower zIndex).
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = frostAlpha }
                .hazeSourceTangem(zIndex = 1f),
        ) {
            TangemFade(
                position = TangemFade.Position.Top,
                variant = TangemFade.Variant.Hard,
                blur = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
        CollectiblesTabBand(
            tabs = tabs,
            scrollState = tabScrollState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier
                .padding(top = topPadding)
                .offset(y = pullOffset),
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesInventoryScreenPreview(
    @PreviewParameter(CollectiblesInventoryScreenPreviewProvider::class) state: CollectiblesInventoryUM,
) {
    TangemThemePreviewRedesign {
        CollectiblesInventoryScreen(
            state = state,
            pullToRefresh = PullToRefreshConfig(isRefreshing = state.isRefreshing, onRefresh = {}),
            onCloseClick = {},
        )
    }
}

private class CollectiblesInventoryScreenPreviewProvider : PreviewParameterProvider<CollectiblesInventoryUM> {

    override val values = sequenceOf(
        CollectiblesInventoryPreviewData.buyback,
        CollectiblesInventoryPreviewData.collection,
        CollectiblesInventoryPreviewData.refreshing,
        CollectiblesInventoryPreviewData.loading,
        CollectiblesInventoryPreviewData.buybackEmpty,
        CollectiblesInventoryPreviewData.collectionEmpty,
        CollectiblesInventoryPreviewData.error,
    )
}