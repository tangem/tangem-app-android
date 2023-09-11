package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.FlowCollector
import kotlin.math.abs

/**
 * Flow collector for scroll items tracking.
 * If first visible item offset is greater than half item size, then [callback] be invoked.
 * If last visible item offset is greater than half item size, then [callback] be invoked.
 *
 * @property lazyListState   lazy list state
 * @property dragInteraction current drag interaction
 * @property callback        lambda be invoked when current scroll items is changed
 *
[REDACTED_AUTHOR]
 */
internal class ScrollOffsetCollector(
    private val lazyListState: LazyListState,
    private val dragInteraction: State<Interaction?>,
    private val callback: (Int) -> Unit,
) : FlowCollector<List<LazyListItemInfo>> {

    private val LazyListItemInfo.halfItemSize get() = size.div(other = 2)

    override suspend fun emit(value: List<LazyListItemInfo>) {
        if (isNotUserInteraction() || value.size <= 1) return

        val firstItem = value.firstOrNull() ?: return
        val lastItem = value.lastOrNull() ?: return

        if (abs(firstItem.offset) > firstItem.halfItemSize) {
            callback(firstItem.index + 1)
        } else if (abs(lastItem.offset) > lastItem.halfItemSize) {
            callback(lastItem.index - 1)
        }
    }

    /**
     * Sometimes the list is scrolled programmatically. Example: selecting a specific wallet when a user opens the
     * screen for the first time or scans a new wallet. Therefore [ScrollOffsetCollector] should not respond to changes.
     */
    private fun isNotUserInteraction(): Boolean {
        return !lazyListState.isScrollInProgress || dragInteraction.value !is DragInteraction.Start
    }
}