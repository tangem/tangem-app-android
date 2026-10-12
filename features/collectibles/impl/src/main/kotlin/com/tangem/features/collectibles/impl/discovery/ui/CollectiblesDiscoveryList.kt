package com.tangem.features.collectibles.impl.discovery.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.common.ui.CollectiblesStatus
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM

internal object DiscoveryListKeys {
    const val TABS = "tabs"
    const val SKELETON = "skeleton"
    const val STATUS = "status"
    const val NEXT_PAGE = "next_page"
    const val CONTENT_TYPE_PACK_ROW = "pack_row"
}

/**
 * The discovery items — tab band, pack rows, status, next-page loader — contributed into the host's lazy list.
 * Frame-scoped rendering inputs come from [LocalCollectiblesDiscoveryListContext], provided by the host.
 */
internal fun LazyListScope.collectiblesDiscoveryList(state: CollectiblesDiscoveryUM, tabScrollState: ScrollState) {
    tabBandItem(tabs = state.tabs, scrollState = tabScrollState)
    contentItems(content = state.content)
}

/**
 * The band is docked once it reaches the top of the viewport; past the band, "no header on screen" means the
 * band has been scrolled far beyond it.
 *
 * @param headerKey key of the host's leading item, laid out before the discovery items
 */
internal fun LazyListState.isCollectiblesTabBandPinned(headerKey: String): Boolean {
    val band = layoutInfo.visibleItemsInfo.firstOrNull { it.key == DiscoveryListKeys.TABS }
    if (band != null) return band.offset <= 0
    return layoutInfo.visibleItemsInfo.none { it.key == headerKey }
}

private fun LazyListScope.tabBandItem(tabs: CollectiblesDiscoveryUM.TabsUM, scrollState: ScrollState) {
    if (tabs is CollectiblesDiscoveryUM.TabsUM.Hidden) return

    item(key = DiscoveryListKeys.TABS, contentType = DiscoveryListKeys.TABS) {
        val context = LocalCollectiblesDiscoveryListContext.current

        if (!context.isTabBandShown) return@item

        val bandModifier = Modifier.alpha(if (context.isTabBandPinned) 0f else context.leadingBlocksAlpha)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f),
        ) {
            if (tabs is CollectiblesDiscoveryUM.TabsUM.Content) {
                CollectiblesTabBand(
                    tabs = tabs.tabs,
                    scrollState = scrollState,
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                    modifier = bandModifier,
                )
            } else {
                CollectiblesTabBandSkeleton(modifier = bandModifier)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Suppress("MagicNumber")
private fun LazyListScope.contentItems(content: CollectiblesDiscoveryUM.ContentUM) {
    when (content) {
        is CollectiblesDiscoveryUM.ContentUM.Loading -> {
            item(key = DiscoveryListKeys.SKELETON, contentType = DiscoveryListKeys.SKELETON) {
                CollectiblesPackGridSkeleton()
            }
        }
        is CollectiblesDiscoveryUM.ContentUM.Content -> {
            items(
                items = content.rows,
                key = { row -> "row_" + row.id },
                contentType = { DiscoveryListKeys.CONTENT_TYPE_PACK_ROW },
            ) { row ->
                PackRow(
                    row = row,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .alpha(if (content.isReloading) 0.4f else 1f),
                )
            }
            if (content.isLoadingNextPage) {
                item(key = DiscoveryListKeys.NEXT_PAGE, contentType = DiscoveryListKeys.NEXT_PAGE) {
                    NextPageLoader()
                }
            }
        }
        is CollectiblesDiscoveryUM.ContentUM.Error -> {
            statusItem {
                CollectiblesStatus(message = content.message, onReloadClick = content.onReloadClick)
            }
        }
        is CollectiblesDiscoveryUM.ContentUM.Empty -> {
            statusItem {
                CollectiblesStatus(message = content.message, onReloadClick = null)
            }
        }
    }
}

@Composable
private fun PackRow(row: CollectiblesDiscoveryUM.PackRowUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        row.packs.forEach { pack ->
            CollectiblesPackCard(state = pack, modifier = Modifier.weight(1f))
        }
        if (row.packs.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** Footer loader shown while the next page is on its way. */
@Composable
private fun NextPageLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader(color = TangemTheme.colors3.icon.primary, size = TangemLoaderSize.X24)
    }
}

private fun LazyListScope.statusItem(content: @Composable BoxScope.() -> Unit) {
    item(key = DiscoveryListKeys.STATUS, contentType = DiscoveryListKeys.STATUS) {
        val statusHeight = LocalCollectiblesDiscoveryListContext.current.statusHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = statusHeight),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}