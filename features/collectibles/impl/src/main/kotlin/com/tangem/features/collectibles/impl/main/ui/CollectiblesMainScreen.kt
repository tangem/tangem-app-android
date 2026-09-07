package com.tangem.features.collectibles.impl.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.discovery.CollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.discovery.ui.isCollectiblesTabBandPinned
import com.tangem.features.collectibles.impl.discovery.ui.preview.CollectiblesDiscoveryPreviewComponent
import com.tangem.features.collectibles.impl.discovery.ui.preview.CollectiblesDiscoveryPreviewData
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesLeadingBlocks
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesMainList
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesMainMenu
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesPinnedHeader
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesSearchFooter
import com.tangem.features.collectibles.impl.main.ui.components.MainListKeys
import com.tangem.features.collectibles.impl.main.ui.components.rememberRestingFooterHeight
import com.tangem.features.collectibles.impl.main.ui.preview.CollectiblesMainPreviewData
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import dev.chrisbanes.haze.rememberHazeState

@Composable
internal fun CollectiblesMainScreen(
    state: CollectiblesMainUM,
    discoveryComponent: CollectiblesDiscoveryComponent,
    discoveryState: CollectiblesDiscoveryUM,
    modifier: Modifier = Modifier,
) {
    val isSearchActive = discoveryState.search.isActive
    val hasTabs = discoveryState.tabs.isNotEmpty()
    val mainListState = rememberLazyListState()
    val searchEntry = remember(isSearchActive, hasTabs) {
        Snapshot.withoutReadObservation {
            createSearchEntry(mainListState = mainListState, hasTabs = hasTabs)
        }
    }
    val listState = if (isSearchActive) searchEntry.listState else mainListState
    val tabScrollState = discoveryComponent.tabScrollState
    val focusRequester = remember { FocusRequester() }
    var footerHeight by remember { mutableIntStateOf(0) }
    val restingFooterHeight = rememberRestingFooterHeight(isSearchActive = isSearchActive, footerHeight = footerHeight)
    val isTabBandPinned by remember(mainListState) {
        derivedStateOf { mainListState.isCollectiblesTabBandPinned(headerKey = MainListKeys.HEADER) }
    }
    val shouldShowPinnedTabs = isTabBandPinned && !isSearchActive && hasTabs

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                title = state.title,
                subtitle = if (isTabBandPinned || isSearchActive) state.balance.fiatPlain else null,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                fadeEnabled = false,
                onBack = state.onBackClick,
                endButtonsGroup = { CollectiblesMainMenu(menu = state.menu) },
            )
        },
        overlay = { contentPadding ->
            CollectiblesPinnedHeader(
                tabs = discoveryState.tabs,
                scrollState = tabScrollState,
                topPadding = contentPadding.calculateTopPadding(),
                isPinned = shouldShowPinnedTabs,
            )
            CollectiblesSearchFooter(
                state = discoveryState.search,
                focusRequester = focusRequester,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeight = size.height },
            )
        },
    ) { contentPadding ->
        val footerInset = if (isSearchActive) footerHeight else restingFooterHeight
        val bottomInset = with(LocalDensity.current) { footerInset.toDp() } + 12.dp

        MaterialBackdrop {
            CollectiblesMainList(
                state = state,
                discoveryComponent = discoveryComponent,
                discoveryState = discoveryState,
                listState = listState,
                lead = CollectiblesLeadingBlocks(
                    isHeaderShown = !isSearchActive || searchEntry.isHeaderShown,
                    isTabBandShown = !isSearchActive || searchEntry.isTabBandShown,
                    isTabBandPinned = isTabBandPinned,
                ),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = bottomInset,
                ),
            )
        }
    }
}

@Composable
private fun MaterialBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val buttonsHazeState = rememberHazeState()
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(TangemTheme.colors3.bg.primary)
                .hazeSourceTangem(state = buttonsHazeState, zIndex = -1f),
        )
        CompositionLocalProvider(LocalHazeState provides buttonsHazeState, content = content)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesMainScreenPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.default)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenZeroPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(
            state = CollectiblesMainPreviewData.zero,
            discoveryState = CollectiblesDiscoveryPreviewData.default,
        )
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenListErrorPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.listError)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenSearchActivePreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.searchActive)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenSearchZeroPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.searchZero)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenSearchErrorPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(discoveryState = CollectiblesDiscoveryPreviewData.searchError)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenMenuPreview() {
    TangemThemePreviewRedesign {
        PreviewScreen(
            state = CollectiblesMainPreviewData.menuShown,
            discoveryState = CollectiblesDiscoveryPreviewData.default,
        )
    }
}

@Composable
private fun PreviewScreen(
    discoveryState: CollectiblesDiscoveryUM,
    state: CollectiblesMainUM = CollectiblesMainPreviewData.default,
) {
    CollectiblesMainScreen(
        state = state,
        discoveryComponent = remember { CollectiblesDiscoveryPreviewComponent(um = discoveryState) },
        discoveryState = discoveryState,
    )
}