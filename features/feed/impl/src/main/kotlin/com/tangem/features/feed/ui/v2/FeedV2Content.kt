package com.tangem.features.feed.ui.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.features.feed.ui.v2.state.FeedV2TabUM
import com.tangem.features.feed.v2.FeedV2Component
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Shtorka 2.0 feed layout: the whole screen scrolls as one — the top blocks collapse away with the
 * tab's list, only the host's sheet header (above) and the tab row stay pinned.
 *
 * The collapse rides nested scroll: scrolling the tab's list up first shrinks the blocks, then the
 * list itself scrolls; scrolling back down re-expands the blocks once the list is at its top. The
 * shtorka's own nested scroll still wins first, raising the sheet before anything collapses.
 *
 * The semi-open (half) detent shows only the blocks and the tab row: the tab's list renders only
 * when [isExpanded], and tapping a tab raises the shtorka to full via [onExpandSheet].
 *
 * @param tabContent the selected tab page, provided by the feed's pager
 */
@Suppress("LongParameterList")
@Composable
internal fun FeedV2Content(
    tabs: ImmutableList<FeedV2TabUM>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    isExpanded: Boolean,
    contentPadding: PaddingValues,
    onExpandSheet: () -> Unit,
    modifier: Modifier = Modifier,
    topBlocks: @Composable (Modifier) -> Unit,
    tabContent: @Composable (Modifier) -> Unit,
) {
    val topBlocksHeightPx = with(LocalDensity.current) { FeedV2Component.TopBlocksHeight.toPx() }
    var topBlocksOffset by rememberSaveable { mutableStateOf(0f) }
    val topBlocksOffsetPx by remember { derivedStateOf { topBlocksOffset.roundToInt() } }

    val topBlocksCollapseConnection = remember(topBlocksHeightPx) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // scrolling up collapses the blocks before the list scrolls
                return if (available.y < 0) collapseBy(available.y) else Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // scrolling down re-expands the blocks after the list has hit its top
                return if (available.y > 0) collapseBy(available.y) else Offset.Zero
            }

            fun collapseBy(delta: Float): Offset {
                val newOffset = (topBlocksOffset + delta).coerceIn(-topBlocksHeightPx, 0f)
                val consumedY = newOffset - topBlocksOffset
                topBlocksOffset = newOffset
                return Offset(x = 0f, y = consumedY)
            }
        }
    }

    // the semi-open detent is sized for expanded blocks — restore them when the sheet drops
    LaunchedEffect(isExpanded) {
        if (!isExpanded) topBlocksOffset = 0f
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
            .nestedScroll(topBlocksCollapseConnection),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                // shrink the laid-out height by the collapse offset and slide the blocks up under
                // the pinned chrome, so the tab row rises as the offset grows
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val height = (placeable.height + topBlocksOffsetPx).coerceAtLeast(0)
                    layout(width = placeable.width, height = height) {
                        placeable.placeRelative(x = 0, y = topBlocksOffsetPx)
                    }
                },
        ) {
            topBlocks(Modifier.fillMaxWidth())
        }
        TangemTabNavigation(
            tabs = rememberTabItems(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabClick = { index ->
                    onTabSelect(index)
                    onExpandSheet()
                },
            ),
            variant = TangemTabItem.Variant.Transparent,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(FeedV2Component.TabRowHeight),
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            tabContent(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun rememberTabItems(
    tabs: ImmutableList<FeedV2TabUM>,
    selectedTabIndex: Int,
    onTabClick: (Int) -> Unit,
): ImmutableList<TangemTabItemUM> {
    val currentOnTabClick by rememberUpdatedState(onTabClick)

    return remember(tabs, selectedTabIndex) {
        tabs
            .mapIndexed { index, tab ->
                TangemTabItemUM.Content(
                    id = tab.id,
                    label = tab.title,
                    isSelected = index == selectedTabIndex,
                    onClick = { currentOnTabClick(index) },
                )
            }
            .toImmutableList()
    }
}