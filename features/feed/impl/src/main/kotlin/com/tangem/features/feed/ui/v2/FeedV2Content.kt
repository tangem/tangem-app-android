package com.tangem.features.feed.ui.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.v2.state.FeedV2TabUM
import com.tangem.features.feed.v2.FeedV2Component
import kotlinx.collections.immutable.ImmutableList

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
                    val height = (placeable.height + topBlocksOffset).coerceAtLeast(0f).roundToInt()
                    layout(width = placeable.width, height = height) {
                        placeable.placeRelative(x = 0, y = topBlocksOffset.roundToInt())
                    }
                },
        ) {
            topBlocks(Modifier.fillMaxWidth())
        }
        FeedTabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelect = { index ->
                onTabSelect(index)
                onExpandSheet()
            },
            // Fixed height so the semi-open detent (blocks + tab row) matches the layout exactly
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
private fun FeedTabRow(
    tabs: ImmutableList<FeedV2TabUM>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            FeedTab(
                tab = tab,
                isSelected = index == selectedTabIndex,
                onClick = { onTabSelect(index) },
            )
        }
    }
}

@Composable
private fun FeedTab(tab: FeedV2TabUM, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(color = if (isSelected) TangemTheme.colors3.bg.tertiary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tab.title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = if (isSelected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
            maxLines = 1,
        )
    }
}