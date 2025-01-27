package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.flow.FlowCollector
import kotlin.math.abs

/**
 * Scrolling detector that changes the current element when scrolling the center of the element.
 * If first visible item offset is greater than half item size, then change selected wallet index.
 * If last visible item offset is greater than half item size, then change selected wallet index.
 *
 * @param initialIndex     initial index
 * @property lazyListState lazy list state
 * @property onIndexChange lambda be invoked when index is changed
 *
[REDACTED_AUTHOR]
 */
internal class CenterOfItemScrollingDetector(
    initialIndex: Int,
    private val lazyListState: LazyListState,
    private val onIndexChange: (Int) -> Unit,
) : FlowCollector<List<LazyListItemData>> {

    private val LazyListItemData.halfItemSize
        get() = size.div(other = 2)

    private var currentIndex = initialIndex

    override suspend fun emit(value: List<LazyListItemData>) {
        if (!lazyListState.isScrollInProgress || value.size <= 1) return

        val firstItem = value.firstOrNull() ?: return
        val lastItem = value.lastOrNull() ?: return

        if (abs(firstItem.offset) > firstItem.halfItemSize) {
            changeIndex(newIndex = firstItem.index + 1)
        } else if (abs(lastItem.offset) > lastItem.halfItemSize) {
            changeIndex(newIndex = lastItem.index - 1)
        }
    }

    private fun changeIndex(newIndex: Int) {
        if (currentIndex != newIndex) {
            currentIndex = newIndex
            onIndexChange(newIndex)
        }
    }
}

internal data class LazyListItemData(val index: Int, val size: Int, val offset: Int)