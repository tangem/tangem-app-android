package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import kotlinx.coroutines.flow.FlowCollector
import kotlin.math.abs

/**
 * Flow collector for scroll items tracking.
 * If first visible item offset is greater than half item size, then change selected wallet index.
 * If last visible item offset is greater than half item size, then change selected wallet index.
 *
 * @property lazyListState     lazy list state
 * @property walletsListConfig wallets list config
 * @property isAutoScroll      check if last scrolling is auto scroll
 *
[REDACTED_AUTHOR]
 */
internal class ScrollOffsetCollector(
    private val lazyListState: LazyListState,
    private val walletsListConfig: WalletsListConfig,
    private val isAutoScroll: State<Boolean>,
) : FlowCollector<List<LazyListItemInfo>> {

    private val LazyListItemInfo.halfItemSize get() = size.div(other = 2)

    private var currentIndex = walletsListConfig.selectedWalletIndex
        set(value) {
            if (field != value) {
                field = value
            }
        }

    override suspend fun emit(value: List<LazyListItemInfo>) {
        // Auto scroll must not change wallet
        if (isAutoScroll.value) {
            currentIndex = walletsListConfig.selectedWalletIndex
            return
        }

        if (!lazyListState.isScrollInProgress || value.size <= 1) return

        val firstItem = value.firstOrNull() ?: return
        val lastItem = value.lastOrNull() ?: return

        if (abs(firstItem.offset) > firstItem.halfItemSize) {
            val newIndex = firstItem.index + 1
            currentIndex = newIndex
            walletsListConfig.onWalletChange(newIndex)
        } else if (abs(lastItem.offset) > lastItem.halfItemSize) {
            val newIndex = lastItem.index - 1
            currentIndex = newIndex
            walletsListConfig.onWalletChange(newIndex)
        }
    }
}