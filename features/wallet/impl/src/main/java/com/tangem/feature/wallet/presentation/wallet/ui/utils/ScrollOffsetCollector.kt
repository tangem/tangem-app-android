package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.foundation.lazy.LazyListItemInfo
import kotlinx.coroutines.flow.FlowCollector
import kotlin.math.abs

/**
 * Flow collector for scroll items tracking.
 * If first visible item offset is greater than half item size, then [callback] be invoked.
 * If last visible item offset is greater than half item size, then [callback] be invoked.
 *
 * @property callback lambda be invoked when current scroll items is changed
 *
* [REDACTED_AUTHOR]
 */
internal class ScrollOffsetCollector(
    private val callback: (Int) -> Unit,
) : FlowCollector<List<LazyListItemInfo>> {

    private val LazyListItemInfo.halfItemSize get() = size.div(other = 2)

    override suspend fun emit(value: List<LazyListItemInfo>) {
        val firstItem = value.firstOrNull() ?: return
        val lastItem = value.lastOrNull() ?: return

        if (abs(firstItem.offset) > firstItem.halfItemSize) {
            callback(firstItem.index + 1)
        } else if (abs(lastItem.offset) > lastItem.halfItemSize) {
            callback(lastItem.index - 1)
        }
    }
}
