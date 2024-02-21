package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.flow.FlowCollector
import kotlin.math.abs

/**
 * Flow collector for scroll items tracking.
 * If first visible item offset is greater than half item size, then change selected wallet index.
 * If last visible item offset is greater than half item size, then change selected wallet index.
 *
 * @param selectedWalletIndex selected wallet index
 * @property lazyListState     lazy list state
 * @property onWalletChange    callback that will be invoked on wallet change
 *
[REDACTED_AUTHOR]
 */
internal class ScrollOffsetCollector(
    selectedWalletIndex: Int,
    private val lazyListState: LazyListState,
    private val onWalletChange: (Int) -> Unit,
) : FlowCollector<List<LazyListItemData>> {

    private val LazyListItemData.halfItemSize
        get() = size.div(other = 2)

    private var currentIndex = selectedWalletIndex

    override suspend fun emit(value: List<LazyListItemData>) {
        if (!lazyListState.isScrollInProgress || value.size <= 1) return

        val firstItem = value.firstOrNull() ?: return
        val lastItem = value.lastOrNull() ?: return

        if (abs(firstItem.offset) > firstItem.halfItemSize) {
            selectIndex(newIndex = firstItem.index + 1)
        } else if (abs(lastItem.offset) > lastItem.halfItemSize) {
            selectIndex(newIndex = lastItem.index - 1)
        }
    }

    private fun selectIndex(newIndex: Int) {
        if (currentIndex != newIndex) {
            currentIndex = newIndex
            onWalletChange(newIndex)
        }
    }
}

internal data class LazyListItemData(
    val index: Int,
    val size: Int,
    val offset: Int,
)