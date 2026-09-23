package com.tangem.features.collectibles.impl.main.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.discovery.CollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.discovery.ui.isCollectiblesTabBandPinned
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesLeadingBlocks
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesMainList
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesMainMenu
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesPinnedHeader
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesSearchFooter
import com.tangem.features.collectibles.impl.main.ui.components.MainListKeys
import com.tangem.features.collectibles.impl.main.ui.components.rememberFillerHeight
import com.tangem.features.collectibles.impl.main.ui.components.rememberListFill
import com.tangem.features.collectibles.impl.main.ui.components.rememberRestingFooterHeight
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM

@Composable
internal fun CollectiblesMainScreen(
    state: CollectiblesMainUM,
    pullToRefresh: PullToRefreshConfig,
    discoveryComponent: CollectiblesDiscoveryComponent,
    discoveryState: CollectiblesDiscoveryUM,
    modifier: Modifier = Modifier,
) {
    val isSearchActive = discoveryState.search.isActive
    val mainListState = rememberLazyListState()
    val searchEntry = remember(isSearchActive, discoveryState.hasTabs) {
        Snapshot.withoutReadObservation {
            createSearchEntry(mainListState = mainListState, hasTabs = discoveryState.hasTabs)
        }
    }
    val listState = if (isSearchActive) searchEntry.listState else mainListState
    val lead = rememberSearchLead(
        mainListState = mainListState,
        searchEntry = searchEntry,
        isSearchActive = isSearchActive,
    )
    val tabScrollState = discoveryComponent.tabScrollState
    val focusRequester = remember { FocusRequester() }
    var footerHeight by remember { mutableIntStateOf(0) }
    val restingFooterHeight = rememberRestingFooterHeight(isSearchActive = isSearchActive, footerHeight = footerHeight)
    val isTabBandPinned by remember(mainListState) {
        derivedStateOf { mainListState.isCollectiblesTabBandPinned(headerKey = MainListKeys.HEADER) }
    }
    val shouldShowPinnedTabs = isTabBandPinned && !isSearchActive && discoveryState.hasTabs

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            CollectiblesTopBar(state = state, isBalanceShown = isTabBandPinned || isSearchActive)
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
                isShown = discoveryState.hasSearchableCatalogue,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeight = size.height },
            )
        },
    ) { contentPadding ->
        val footerInset = if (isSearchActive) footerHeight else restingFooterHeight
        val bottomInset = with(LocalDensity.current) { footerInset.toDp() } + 12.dp

        RefreshableList(
            state = state,
            pullToRefresh = pullToRefresh,
            discoveryComponent = discoveryComponent,
            discoveryState = discoveryState,
            listState = listState,
            lead = CollectiblesLeadingBlocks(
                areShown = lead.areShown,
                isTabBandPinned = isTabBandPinned,
                alpha = lead.alpha,
            ),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = bottomInset,
            ),
        )
    }
}

@Composable
private fun CollectiblesTopBar(state: CollectiblesMainUM, isBalanceShown: Boolean) {
    TangemTopNavigation(
        title = state.title,
        subtitle = if (isBalanceShown) state.balance.fiatPlain else null,
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        fadeEnabled = false,
        onBack = state.onBackClick,
        endButton = { CollectiblesMainMenu(menu = state.menu) },
    )
}

@Suppress("LongParameterList")
@Composable
private fun RefreshableList(
    state: CollectiblesMainUM,
    pullToRefresh: PullToRefreshConfig,
    discoveryComponent: CollectiblesDiscoveryComponent,
    discoveryState: CollectiblesDiscoveryUM,
    listState: LazyListState,
    lead: CollectiblesLeadingBlocks,
    contentPadding: PaddingValues,
) {
    val isSearchActive = discoveryState.search.isActive

    val fill = rememberListFill(
        listState = listState,
        rowCount = (discoveryState.content as? CollectiblesDiscoveryUM.ContentUM.Content)?.rows?.size ?: 0,
        hasTabs = discoveryState.hasTabs,
    )
    val fillerHeight = rememberFillerHeight(
        fill = fill,
        isSearchActive = isSearchActive,
        isEnteringSearch = lead.areShown,
    )

    if (isSearchActive) {
        CollectiblesMainList(
            state = state,
            discoveryComponent = discoveryComponent,
            discoveryState = discoveryState,
            listState = listState,
            lead = lead,
            fillerHeight = fillerHeight,
            statusHeight = fill.statusHeight,
            contentPadding = contentPadding,
        )
    } else {
        TangemPullToRefreshSlidingContainer(
            config = pullToRefresh,
            indicatorOffset = contentPadding.calculateTopPadding(),
        ) {
            CollectiblesMainList(
                state = state,
                discoveryComponent = discoveryComponent,
                discoveryState = discoveryState,
                listState = listState,
                lead = lead,
                fillerHeight = fillerHeight,
                statusHeight = fill.statusHeight,
                contentPadding = contentPadding,
            )
        }
    }
}

private val CollectiblesDiscoveryUM.hasTabs: Boolean
    get() = tabs !is CollectiblesDiscoveryUM.TabsUM.Hidden