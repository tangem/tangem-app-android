package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import com.tangem.utils.extensions.mapNotNullValues

/**
 * Animate scroll [LazyListState].
 *
 * [LazyListState] method for scroll with animation by index isn't supported custom animation.
 * This extension method calculate offset between [prevIndex] and [newIndex],
 * and scroll by it with default animation.
 */
internal suspend fun LazyListState.animateScrollByIndex(prevIndex: Int, newIndex: Int) {
    animateScrollBy(
        value = calculateOffset(layoutInfo, prevIndex, newIndex),
        animationSpec = tween(durationMillis = 1000),
    )
}

private fun calculateOffset(layoutInfo: LazyListLayoutInfo, prevIndex: Int, newIndex: Int): Float {
    val indexDifference = newIndex - prevIndex
    val coefficient = if (indexDifference == 0) 1 else indexDifference

    return layoutInfo.getItemSizeWithSpacing().times(other = coefficient).toFloat()
}

private fun LazyListLayoutInfo.getItemSizeWithSpacing(): Int {
    return viewportSize.width - afterContentPadding - beforeContentPadding + mainAxisItemSpacing
}

/**
 * Saver for [LazyListState] map, where key is page index, and value is [LazyListState] of this page.
 */
internal fun lazyListStateMapSaver(pageCount: Int): Saver<MutableMap<Int, LazyListState>, Any> {
    return mapSaver(
        save = { map ->
            map.mapKeys { it.key.toString() }
                .mapValues { listState ->
                    listState.value.firstVisibleItemIndex to listState.value.firstVisibleItemScrollOffset
                }
        },
        restore = { restoredMap ->
            @Suppress("UNCHECKED_CAST")
            val typedMap = restoredMap as? Map<String, Pair<Int, Int>> ?: return@mapSaver null

            typedMap.mapKeys { it.key.toInt() }
                .mapNotNullValues { (_, value) ->
                    val (index, offset) = value
                    LazyListState(index, offset)
                }
                .toMutableMap()
                .apply {
                    repeat(pageCount) { putIfAbsent(it, LazyListState()) }
                }
        },
    )
}