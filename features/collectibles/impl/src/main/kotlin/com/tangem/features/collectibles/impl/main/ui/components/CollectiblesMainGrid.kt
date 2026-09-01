package com.tangem.features.collectibles.impl.main.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import kotlinx.collections.immutable.ImmutableList

internal const val GRID_COLUMNS = 2

internal object GridKeys {
    const val HEADER = "header"
    const val TABS = "tabs"
    const val STATUS = "status"
    const val FILLER = "filler"
    const val CONTENT_TYPE_PACK = "pack"
}

private const val CONTENT_FADE_IN_MILLIS = 300
private const val CONTENT_FADE_OUT_MILLIS = 250

internal data class CollectiblesLeadingBlocks(
    val isHeaderShown: Boolean,
    val isTabBandShown: Boolean,
    val isTabBandPinned: Boolean,
)

@Composable
internal fun CollectiblesGrid(
    state: CollectiblesMainUM,
    gridState: LazyGridState,
    tabScrollState: ScrollState,
    lead: CollectiblesLeadingBlocks,
    contentPadding: PaddingValues,
) {
    val packCount = (state.content as? CollectiblesMainUM.ContentUM.Content)?.packs?.size ?: 0
    val isSearchActive = state.search.isActive
    val fill = rememberGridFill(
        gridState = gridState,
        packCount = packCount,
        hasTabs = state.tabs.isNotEmpty(),
    )

    // Frozen at what the main list had: the search grid must start with the very same total height, or the
    // copied scroll position gets clamped and the fold begins somewhere else entirely.
    var restingFillerHeight by remember { mutableStateOf(0.dp) }
    val fillerHeight = if (isSearchActive) restingFillerHeight else fill.fillerHeight

    LaunchedEffect(isSearchActive, fill.fillerHeight) {
        if (!isSearchActive) restingFillerHeight = fill.fillerHeight
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (lead.isHeaderShown) {
            headerItem(state = state, isFolding = isSearchActive)
        }
        if (lead.isTabBandShown) {
            tabBandItem(
                tabs = state.tabs,
                scrollState = tabScrollState,
                isPinned = lead.isTabBandPinned,
                isFolding = isSearchActive,
            )
        }
        contentItems(content = state.content, statusHeight = fill.statusHeight)

        if (state.content is CollectiblesMainUM.ContentUM.Content) {
            fillerItem(height = fillerHeight, isFolding = isSearchActive)
        }
    }
}

internal fun LazyGridState.isTabBandPinned(): Boolean {
    val band = layoutInfo.visibleItemsInfo.firstOrNull { it.key == GridKeys.TABS }
    if (band != null) return band.offset.y <= 0
    return layoutInfo.visibleItemsInfo.none { it.key == GridKeys.HEADER }
}

private fun LazyGridScope.headerItem(state: CollectiblesMainUM, isFolding: Boolean) {
    item(
        key = GridKeys.HEADER,
        span = { GridItemSpan(maxLineSpan) },
        contentType = GridKeys.HEADER,
    ) {
        FoldingBlock(isFolding = isFolding) {
            CollectiblesBalanceBlock(balance = state.balance)
            CollectiblesActionsBlock(actions = state.actions)
            state.collection?.let { collection -> CollectiblesCollectionWidget(state = collection) }
        }
    }
}

private fun LazyGridScope.tabBandItem(
    tabs: ImmutableList<TangemTabItemUM>,
    scrollState: ScrollState,
    isPinned: Boolean,
    isFolding: Boolean,
) {
    if (tabs.isEmpty()) return

    item(
        key = GridKeys.TABS,
        span = { GridItemSpan(maxLineSpan) },
        contentType = GridKeys.TABS,
    ) {
        Column {
            FoldingBlock(isFolding = isFolding) {
                if (isPinned) {
                    Spacer(modifier = Modifier.height(TabBandHeight))
                } else {
                    CollectiblesTabBand(
                        tabs = tabs,
                        scrollState = scrollState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(TabBandBottomGap))
        }
    }
}

private fun LazyGridScope.contentItems(content: CollectiblesMainUM.ContentUM, statusHeight: Dp) {
    when (content) {
        is CollectiblesMainUM.ContentUM.Loading -> {
            statusItem(statusHeight = statusHeight) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TangemLoader(color = TangemTheme.colors3.icon.primary, size = TangemLoaderSize.X32)
                }
            }
        }
        is CollectiblesMainUM.ContentUM.Content -> {
            items(items = content.packs, key = { it.id }, contentType = { GridKeys.CONTENT_TYPE_PACK }) { pack ->
                CollectiblesPackCard(
                    state = pack,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = tween(durationMillis = CONTENT_FADE_IN_MILLIS),
                            fadeOutSpec = tween(durationMillis = CONTENT_FADE_OUT_MILLIS),
                        )
                        .padding(bottom = 24.dp),
                )
            }
        }
        is CollectiblesMainUM.ContentUM.Error -> {
            statusItem(statusHeight = statusHeight) {
                CollectiblesMainStatus(message = content.message, onReloadClick = content.onReloadClick)
            }
        }
        is CollectiblesMainUM.ContentUM.Empty -> {
            statusItem(statusHeight = statusHeight) {
                CollectiblesMainStatus(message = content.message, onReloadClick = null)
            }
        }
    }
}

private fun LazyGridScope.statusItem(statusHeight: Dp, content: @Composable BoxScope.() -> Unit) {
    item(key = GridKeys.STATUS, span = { GridItemSpan(maxLineSpan) }, contentType = GridKeys.STATUS) {
        Box(
            modifier = Modifier
                .animateItem(
                    fadeInSpec = tween(durationMillis = CONTENT_FADE_IN_MILLIS),
                    fadeOutSpec = tween(durationMillis = CONTENT_FADE_OUT_MILLIS),
                )
                .heightIn(min = statusHeight),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

private fun LazyGridScope.fillerItem(height: Dp, isFolding: Boolean) {
    item(
        key = GridKeys.FILLER,
        span = { GridItemSpan(maxLineSpan) },
        contentType = GridKeys.FILLER,
    ) {
        FoldingBlock(isFolding = isFolding) {
            Spacer(modifier = Modifier.height(height))
        }
    }
}