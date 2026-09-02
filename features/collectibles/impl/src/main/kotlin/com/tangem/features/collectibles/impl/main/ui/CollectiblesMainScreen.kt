package com.tangem.features.collectibles.impl.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
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
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesGrid
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesLeadingBlocks
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesMainMenu
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesPinnedHeader
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesSearchFooter
import com.tangem.features.collectibles.impl.main.ui.components.isTabBandPinned
import com.tangem.features.collectibles.impl.main.ui.components.rememberRestingFooterHeight
import com.tangem.features.collectibles.impl.main.ui.preview.CollectiblesMainPreviewData
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import dev.chrisbanes.haze.rememberHazeState

@Composable
internal fun CollectiblesMainScreen(state: CollectiblesMainUM, modifier: Modifier = Modifier) {
    val isSearchActive = state.search.isActive
    val mainGridState = rememberLazyGridState()
    val searchEntry = remember(isSearchActive, state.tabs.isEmpty()) {
        Snapshot.withoutReadObservation {
            createSearchEntry(mainGridState = mainGridState, hasTabs = state.tabs.isNotEmpty())
        }
    }
    val searchGridState = searchEntry.gridState
    val gridState = if (isSearchActive) searchGridState else mainGridState
    val tabScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    var footerHeight by remember { mutableIntStateOf(0) }
    val restingFooterHeight = rememberRestingFooterHeight(isSearchActive = isSearchActive, footerHeight = footerHeight)
    val isTabBandPinned by remember(mainGridState) { derivedStateOf { mainGridState.isTabBandPinned() } }
    val shouldShowPinnedTabs = isTabBandPinned && !isSearchActive && state.tabs.isNotEmpty()

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
                tabs = state.tabs,
                scrollState = tabScrollState,
                topPadding = contentPadding.calculateTopPadding(),
                isPinned = shouldShowPinnedTabs,
            )
            CollectiblesSearchFooter(
                state = state.search,
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
            CollectiblesGrid(
                state = state,
                gridState = gridState,
                tabScrollState = tabScrollState,
                lead = CollectiblesLeadingBlocks(
                    isHeaderShown = !isSearchActive || searchEntry.isHeaderShown,
                    isTabBandShown = !isSearchActive || searchEntry.isTabBandShown,
                    isTabBandPinned = isTabBandPinned,
                ),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = bottomInset,
                    start = 16.dp,
                    end = 16.dp,
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
        CollectiblesMainScreen(state = CollectiblesMainPreviewData.default)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenZeroPreview() {
    TangemThemePreviewRedesign {
        CollectiblesMainScreen(state = CollectiblesMainPreviewData.zero)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenListErrorPreview() {
    TangemThemePreviewRedesign {
        CollectiblesMainScreen(state = CollectiblesMainPreviewData.listError)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenSearchActivePreview() {
    TangemThemePreviewRedesign {
        CollectiblesMainScreen(state = CollectiblesMainPreviewData.searchActive)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenSearchZeroPreview() {
    TangemThemePreviewRedesign {
        CollectiblesMainScreen(state = CollectiblesMainPreviewData.searchZero)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenSearchErrorPreview() {
    TangemThemePreviewRedesign {
        CollectiblesMainScreen(state = CollectiblesMainPreviewData.searchError)
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun CollectiblesMainScreenMenuPreview() {
    TangemThemePreviewRedesign {
        CollectiblesMainScreen(state = CollectiblesMainPreviewData.menuShown)
    }
}