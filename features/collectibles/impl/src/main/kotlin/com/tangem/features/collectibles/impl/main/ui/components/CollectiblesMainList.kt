package com.tangem.features.collectibles.impl.main.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.features.collectibles.impl.common.ui.FoldingBlock
import com.tangem.features.collectibles.impl.discovery.CollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.discovery.ui.CollectiblesDiscoveryListContext
import com.tangem.features.collectibles.impl.discovery.ui.CollectiblesLoadMoreEffect
import com.tangem.features.collectibles.impl.discovery.ui.LocalCollectiblesDiscoveryListContext
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM

internal object MainListKeys {
    const val HEADER = "header"
    const val FILLER = "filler"
}

internal data class CollectiblesLeadingBlocks(
    val isHeaderShown: Boolean,
    val isTabBandShown: Boolean,
    val isTabBandPinned: Boolean,
)

@Suppress("LongParameterList")
@Composable
internal fun CollectiblesMainList(
    state: CollectiblesMainUM,
    discoveryComponent: CollectiblesDiscoveryComponent,
    discoveryState: CollectiblesDiscoveryUM,
    listState: LazyListState,
    lead: CollectiblesLeadingBlocks,
    contentPadding: PaddingValues,
) {
    val rowCount = (discoveryState.content as? CollectiblesDiscoveryUM.ContentUM.Content)?.rows?.size ?: 0
    val isSearchActive = discoveryState.search.isActive
    val fill = rememberListFill(
        listState = listState,
        rowCount = rowCount,
        hasTabs = discoveryState.tabs.isNotEmpty(),
    )

    // Frozen at what the main list had: the search list must start with the very same total height, or the
    // copied scroll position gets clamped and the fold begins somewhere else entirely.
    var restingFillerHeight by remember { mutableStateOf(0.dp) }
    val fillerHeight = if (isSearchActive) restingFillerHeight else fill.fillerHeight

    LaunchedEffect(isSearchActive, fill.fillerHeight) {
        if (!isSearchActive) restingFillerHeight = fill.fillerHeight
    }

    CollectiblesLoadMoreEffect(
        listState = listState,
        isEnabled = (discoveryState.content as? CollectiblesDiscoveryUM.ContentUM.Content)?.isReloading == false,
        onLoadMore = discoveryComponent::onLoadMore,
    )

    val discoveryListContext = CollectiblesDiscoveryListContext(
        isTabBandPinned = lead.isTabBandPinned,
        isTabBandShown = lead.isTabBandShown,
        statusHeight = fill.statusHeight,
    )

    CompositionLocalProvider(LocalCollectiblesDiscoveryListContext provides discoveryListContext) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
        ) {
            headerItem(state = state, isHeaderShown = lead.isHeaderShown, isFolding = isSearchActive)

            with(discoveryComponent) { content(uiState = discoveryState, modifier = Modifier) }

            if (discoveryState.content is CollectiblesDiscoveryUM.ContentUM.Content) {
                fillerItem(height = fillerHeight, isFolding = isSearchActive)
            }
        }
    }
}

private fun LazyListScope.headerItem(state: CollectiblesMainUM, isHeaderShown: Boolean, isFolding: Boolean) {
    item(
        key = MainListKeys.HEADER,
        contentType = MainListKeys.HEADER,
    ) {
        // Rendered zero-height rather than omitted, so a scroll position copied from the resting list keeps
        // pointing at the same item.
        if (!isHeaderShown) return@item

        FoldingBlock(isFolding = isFolding) {
            CollectiblesBalanceBlock(balance = state.balance)
            CollectiblesActionsBlock(actions = state.actions)
            state.collection?.let { collection -> CollectiblesCollectionWidget(state = collection) }
        }
    }
}

private fun LazyListScope.fillerItem(height: Dp, isFolding: Boolean) {
    item(
        key = MainListKeys.FILLER,
        contentType = MainListKeys.FILLER,
    ) {
        FoldingBlock(isFolding = isFolding) {
            Spacer(modifier = Modifier.height(height))
        }
    }
}