package com.tangem.features.earn.impl.info.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.features.earn.impl.info.label
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Item offsets start under the navigation where the chips are pinned, so a section scrolled to `-chipsHeightPx` sits
 * right under them.
 */
@Composable
internal fun rememberSectionChips(
    tabs: ImmutableList<EarnInfoTab>,
    sectionIndices: Map<EarnInfoTab, Int>,
    listState: LazyListState,
    chipsHeightPx: () -> Int,
): ImmutableList<TangemTabItemUM> {
    val scope = rememberCoroutineScope()
    val clickedSection = remember(sectionIndices) { ClickedSection() }
    LaunchedEffect(clickedSection, listState) {
        snapshotFlow { listState.position() }.collect(clickedSection::onPositionChanged)
    }
    val scrolledTab by remember(sectionIndices) {
        derivedStateOf {
            if (sectionIndices.isEmpty()) {
                null
            } else {
                listState.scrolledSection(sectionIndices = sectionIndices, chipsBottom = chipsHeightPx())
            }
        }
    }
    val selected = clickedSection.tab ?: scrolledTab

    return remember(tabs, sectionIndices, selected) {
        tabs.map { tab ->
            TangemTabItemUM.Content(
                id = tab.name,
                label = tab.label(),
                isSelected = tab == selected,
                onClick = {
                    val tapId = clickedSection.onTap(tab)
                    scope.launch {
                        try {
                            listState.animateScrollToItem(
                                index = sectionIndices.getValue(tab),
                                scrollOffset = -chipsHeightPx(),
                            )
                            clickedSection.onScrollEnded(tapId = tapId, position = listState.position())
                        } catch (e: CancellationException) {
                            clickedSection.onScrollInterrupted(tapId)
                            throw e
                        }
                    }
                },
            )
        }.toImmutableList()
    }
}

private fun LazyListState.scrolledSection(sectionIndices: Map<EarnInfoTab, Int>, chipsBottom: Int): EarnInfoTab {
    val tops = sectionTops(
        sectionIndices = sectionIndices,
        visibleOffsets = layoutInfo.visibleItemsInfo.associate { item -> item.index to item.offset },
        firstVisibleIndex = firstVisibleItemIndex,
    )

    return selectedSection(
        sectionTops = tops,
        referenceLine = referenceLine(
            chipsBottom = chipsBottom,
            viewportHeight = visibleListHeight(
                viewportHeight = layoutInfo.viewportSize.height,
                beforeContentPadding = layoutInfo.beforeContentPadding,
                afterContentPadding = layoutInfo.afterContentPadding,
            ),
        ),
        isAtEnd = canScrollBackward && !canScrollForward,
    )
}

internal fun LazyListState.hasPinnedChips(chipsIndex: Int): Boolean = areChipsPinned(
    chipsOffset = itemOffset(chipsIndex),
    chipsIndex = chipsIndex,
    firstVisibleIndex = firstVisibleItemIndex,
)

internal fun LazyListState.floatingChipsTop(chipsIndex: Int, navigationHeight: Int): Int = chipsRowTop(
    chipsOffset = itemOffset(chipsIndex),
    chipsIndex = chipsIndex,
    firstVisibleIndex = firstVisibleItemIndex,
    navigationHeight = navigationHeight,
    viewportHeight = layoutInfo.viewportSize.height,
)

private fun LazyListState.itemOffset(index: Int): Int? =
    layoutInfo.visibleItemsInfo.firstOrNull { item -> item.index == index }?.offset

private fun LazyListState.position(): ListPosition = ListPosition(
    index = firstVisibleItemIndex,
    scrollOffset = firstVisibleItemScrollOffset,
)