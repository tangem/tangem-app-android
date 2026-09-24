package com.tangem.features.feed.ui.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.features.feed.components.v2.blocks.FeedBlock
import com.tangem.features.feed.v2.FeedV2Component
import kotlinx.collections.immutable.ImmutableList

private val BlockGap = 8.dp
private val RowHorizontalPadding = 16.dp

// Derived from the api-declared row height so the semi-open detent matches the layout exactly.
private val LargeBlockHeight = FeedV2Component.TopBlocksHeight
private val SmallBlockHeight = (LargeBlockHeight - BlockGap) / 2
private val SmallBlockWidth = 120.dp

/** How many small blocks share a column before the next one starts a new one. */
private const val SMALL_BLOCKS_PER_COLUMN = 2

/**
 * Top-blocks row: the contributed [blocks] in the order they were given, each drawing itself.
 *
 * Small blocks stack into columns of [SMALL_BLOCKS_PER_COLUMN]; large ones stand alone. A lone large
 * card stretches across whatever the rest leaves free, since there is nothing to scroll to; from two
 * on they take their fixed width and the row scrolls.
 */
@Composable
internal fun FeedTopBlocksContent(
    blocks: ImmutableList<FeedBlock>,
    onBlocksOnScreenChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = remember(blocks) { blocks.toColumns() }
    val listState = rememberLazyListState()

    BlocksOnScreenTracker(listState = listState, onBlocksOnScreenChange = onBlocksOnScreenChange)

    BoxWithConstraints(modifier = modifier) {
        val largeBlockWidth = rememberLargeBlockWidth(columns = columns, rowWidth = maxWidth)

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = RowHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(space = BlockGap),
        ) {
            columns.forEach { column ->
                item(key = column.first().key) {
                    Column(verticalArrangement = Arrangement.spacedBy(space = BlockGap)) {
                        column.forEach { block ->
                            block.Content(modifier = block.sizeModifier(largeBlockWidth = largeBlockWidth))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlocksOnScreenTracker(listState: LazyListState, onBlocksOnScreenChange: (Set<String>) -> Unit) {
    val keysOnScreen by remember(listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNullTo(destination = mutableSetOf()) { it.key as? String }
        }
    }
    val onChange by rememberUpdatedState(onBlocksOnScreenChange)

    LaunchedEffect(keysOnScreen) { onChange(keysOnScreen) }

    DisposableEffect(listState) { onDispose { onChange(emptySet()) } }
}

/**
 * A single large card has the row to itself once the small ones have taken their column, so it fills
 * the rest instead of leaving a gap no one can scroll into.
 */
@Composable
private fun rememberLargeBlockWidth(columns: List<List<FeedBlock>>, rowWidth: Dp): Dp = remember(columns, rowWidth) {
    val largeColumns = columns.count { column -> column.first().size == FeedBlock.Size.Large }
    if (largeColumns != 1) return@remember 186.dp

    val smallColumns = columns.size - largeColumns
    val takenBySmall = (SmallBlockWidth + BlockGap) * smallColumns
    rowWidth - RowHorizontalPadding * 2 - takenBySmall
}

private fun FeedBlock.sizeModifier(largeBlockWidth: Dp): Modifier = when (size) {
    FeedBlock.Size.Small -> Modifier.size(width = SmallBlockWidth, height = SmallBlockHeight)
    FeedBlock.Size.Large -> Modifier.size(width = largeBlockWidth, height = LargeBlockHeight)
}

/** Runs of small blocks become columns; a large block always gets a column of its own. */
private fun List<FeedBlock>.toColumns(): List<List<FeedBlock>> = buildList {
    val pendingSmall = mutableListOf<FeedBlock>()

    fun flushSmall() {
        if (pendingSmall.isNotEmpty()) {
            add(pendingSmall.toList())
            pendingSmall.clear()
        }
    }

    this@toColumns.forEach { block ->
        if (block.size == FeedBlock.Size.Large) {
            flushSmall()
            add(listOf(block))
        } else {
            pendingSmall.add(block)
            if (pendingSmall.size == SMALL_BLOCKS_PER_COLUMN) flushSmall()
        }
    }

    flushSmall()
}