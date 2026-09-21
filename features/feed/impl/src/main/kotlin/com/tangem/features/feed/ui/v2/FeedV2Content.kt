package com.tangem.features.feed.ui.v2

import androidx.compose.animation.core.animate
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.features.feed.ui.FeedTabRow
import com.tangem.features.feed.ui.FeedTabUM
import com.tangem.features.feed.v2.FeedV2Component
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Shtorka 2.0 feed layout: the whole screen scrolls as one — the top blocks collapse away with the
 * tab's list, only the host's sheet header (above) and the tab row stay pinned.
 *
 * The collapse rides nested scroll: scrolling the tab's list up first shrinks the blocks, then the
 * list itself scrolls; scrolling back down re-expands the blocks once the list is at its top. The
 * shtorka's own nested scroll still wins first, raising the sheet before anything collapses.
 *
 * @param collapseTopBlocksRequests blocks collapse on request too, when one of them opens a tab
 * @param tabContent the selected tab page, provided by the feed's pager
 */
@Suppress("LongParameterList")
@Composable
internal fun FeedV2Content(
    tabs: ImmutableList<FeedTabUM>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    isExpanded: Boolean,
    contentPadding: PaddingValues,
    onExpandSheet: () -> Unit,
    collapseTopBlocksRequests: Flow<Unit>,
    modifier: Modifier = Modifier,
    topBlocks: @Composable (Modifier) -> Unit,
    tabContent: @Composable (Modifier, PaddingValues) -> Unit,
) {
    val topBlocksHeightPx = with(LocalDensity.current) { FeedV2Component.TopBlocksHeight.toPx() }
    val topBlocksOffsetState = rememberSaveable { mutableStateOf(0f) }
    val topBlocksOffset by topBlocksOffsetState

    val chromeOffsetState = rememberChromeOffset(topBlocksOffsetState, topBlocksHeightPx)
    val chromeOffset by chromeOffsetState

    val topBlocksCollapseConnection = remember(topBlocksHeightPx) {
        TopBlocksCollapseConnection(topBlocksHeightPx = topBlocksHeightPx, offsetState = topBlocksOffsetState)
    }

    // the two collapse animations below can be asked for at once — dropping the sheet mid-collapse
    // starts the re-expansion — and the later one must take the offset over, not fight for it
    val collapseMutex = remember { MutatorMutex() }

    // the semi-open detent is sized for expanded blocks — ease them back when the sheet drops;
    // a snap here would jump the tab row mid-gesture, since isExpanded flips while dragging
    LaunchedEffect(isExpanded) {
        if (!isExpanded && topBlocksOffset != 0f) {
            collapseMutex.mutate { topBlocksOffsetState.animateOffsetTo(target = 0f) }
        }
    }

    // a tab opened from a block leaves the row behind: the sheet rises to the height the collapsed
    // layout is sized for, and the blocks travel the same way a scroll up would take them
    val currentOnExpandSheet by rememberUpdatedState(onExpandSheet)
    LaunchedEffect(collapseTopBlocksRequests, topBlocksHeightPx) {
        collapseTopBlocksRequests.collect {
            currentOnExpandSheet()
            collapseMutex.mutate { topBlocksOffsetState.animateOffsetTo(target = -topBlocksHeightPx) }
        }
    }

    val feedHazeState = rememberFeedHazeState()
    val tabContentPadding = rememberTabContentPadding(
        contentPadding = contentPadding,
        collapseOffsetPx = chromeOffsetState,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(topBlocksCollapseConnection),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(state = feedHazeState),
        ) {
            // No overscroll in the tabs: a downward fling at the list's top must hand its velocity
            // to the shtorka settle; the stretch effect would swallow it into a giant stretch that
            // displaces the rendered rows for a second (the list looks torn/blank while it relaxes).
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                tabContent(Modifier.fillMaxSize(), tabContentPadding)
            }
            CollapsingTopBlocks(
                offsetState = topBlocksOffsetState,
                heightPx = topBlocksHeightPx,
                contentPadding = contentPadding,
                content = topBlocks,
            )
        }
        FeedChromeOverlay(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelect = { index ->
                onTabSelect(index)
                onExpandSheet()
            },
            contentPadding = contentPadding,
            hazeState = feedHazeState,
            fadeAlpha = { -topBlocksOffset / topBlocksHeightPx },
            tabRowOffsetPx = { chromeOffset.roundToInt() },
        )
    }
}

private suspend fun MutableState<Float>.animateOffsetTo(target: Float) {
    animate(initialValue = value, targetValue = target) { animatedValue, _ -> value = animatedValue }
}

/**
 * The top blocks as the collapse moves them: they slide up by the offset, the space they leave is
 * taken off their measured height so the tab content follows, and they fade out at the end of the
 * travel. The offset is read inside the draw and layout lambdas, so a scroll frame never recomposes.
 */
@Composable
private fun BoxScope.CollapsingTopBlocks(
    offsetState: State<Float>,
    heightPx: Float,
    contentPadding: PaddingValues,
    content: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = contentPadding.calculateTopPadding())
            .fillMaxWidth()
            .graphicsLayer {
                val fraction = -offsetState.value / heightPx
                // The blocks fade out over the trailing quarter of the collapse

                @Suppress("MagicNumber")
                val exitAlpha = ((1f - fraction) / 0.25f).coerceIn(0f, 1f)
                alpha = exitAlpha
            }
            .layout { measurable, constraints ->
                val offsetPx = offsetState.value.roundToInt()
                val placeable = measurable.measure(constraints)
                val height = (placeable.height + offsetPx).coerceAtLeast(0)
                layout(width = placeable.width, height = height) {
                    placeable.placeRelative(x = 0, y = offsetPx)
                }
            },
    ) {
        content(Modifier.fillMaxWidth())
    }
}

@Suppress("LongParameterList")
@Composable
private fun BoxScope.FeedChromeOverlay(
    tabs: ImmutableList<FeedTabUM>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    contentPadding: PaddingValues,
    hazeState: HazeState,
    fadeAlpha: () -> Float,
    tabRowOffsetPx: () -> Int,
) {
    CompositionLocalProvider(LocalHazeState provides hazeState) {
        TangemFade(
            position = TangemFade.Position.Top,
            variant = TangemFade.Variant.Hard,
            blur = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(contentPadding.calculateTopPadding() + FeedV2Component.TabRowHeight)
                .hazeSourceTangem(zIndex = 1f)
                .graphicsLayer { alpha = fadeAlpha() },
        )
        FeedTabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelect = onTabSelect,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    top = contentPadding.calculateTopPadding() +
                        FeedV2Component.TopBlocksHeight +
                        FeedV2Component.TabRowTopGap,
                )
                .offset { IntOffset(x = 0, y = tabRowOffsetPx()) },
        )
    }
}

/**
 * Collapse offset of the chrome below the top blocks: the gap above the tab row closes together with
 * the blocks, so the chrome travels the block height plus the gap while the blocks travel only their own.
 */
@Composable
private fun rememberChromeOffset(topBlocksOffset: State<Float>, topBlocksHeightPx: Float): State<Float> {
    val tabRowGapPx = with(LocalDensity.current) { FeedV2Component.TabRowTopGap.toPx() }
    return remember(topBlocksOffset, topBlocksHeightPx, tabRowGapPx) {
        derivedStateOf { topBlocksOffset.value * (topBlocksHeightPx + tabRowGapPx) / topBlocksHeightPx }
    }
}

@Composable
private fun rememberTabContentPadding(contentPadding: PaddingValues, collapseOffsetPx: State<Float>): PaddingValues {
    val density = LocalDensity.current
    return remember(density, contentPadding, collapseOffsetPx) {
        CollapsingContentPadding(
            restTop = contentPadding.calculateTopPadding() +
                FeedV2Component.TopBlocksHeight +
                FeedV2Component.TabRowTopGap +
                FeedV2Component.TabRowHeight,
            density = density,
            collapseOffsetPx = collapseOffsetPx,
        )
    }
}

/** The offset is read in [calculateTopPadding] at measure time, so lists track the collapse without recomposing. */
@Stable
private class CollapsingContentPadding(
    private val restTop: Dp,
    private val density: Density,
    private val collapseOffsetPx: State<Float>,
) : PaddingValues {

    override fun calculateTopPadding(): Dp = restTop + with(density) { collapseOffsetPx.value.toDp() }

    override fun calculateBottomPadding(): Dp = 0.dp

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp
}

/** Feed-local haze source — [LocalHazeState] here would sample the screen behind the card instead. */
@Composable
private fun rememberFeedHazeState(): HazeState {
    val outerHazeState = LocalHazeState.current
    val feedHazeState = rememberHazeState()
    SideEffect { feedHazeState.blurEnabled = outerHazeState.blurEnabled }
    return feedHazeState
}

/**
 * Drives the top-blocks collapse: scrolling up collapses the blocks before the list scrolls
 * ([onPreScroll]), scrolling down re-expands them once the list has hit its top ([onPostScroll]).
 */
private class TopBlocksCollapseConnection(
    private val topBlocksHeightPx: Float,
    private val offsetState: MutableState<Float>,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return if (available.y < 0) collapseBy(available.y) else Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return if (available.y > 0) collapseBy(available.y) else Offset.Zero
    }

    private fun collapseBy(delta: Float): Offset {
        val newOffset = (offsetState.value + delta).coerceIn(-topBlocksHeightPx, 0f)
        val consumedY = newOffset - offsetState.value
        offsetState.value = newOffset
        return Offset(x = 0f, y = consumedY)
    }
}