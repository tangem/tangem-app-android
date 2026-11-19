package com.tangem.features.feed.rwa.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.features.feed.rwa.model.list.RwaInterval
import com.tangem.features.feed.rwa.ui.state.RwaIntervalUM

/**
 * Trend-interval options dropped under the interval chip, mirroring the Crypto tab's menu.
 *
 * Picking an option dismisses the menu itself; the model only ever sees the new interval.
 */
@Composable
internal fun RwaIntervalMenu(
    um: RwaIntervalUM,
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemMenu(
        isExpanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        isSelectable = true,
    ) {
        RwaInterval.entries.fastForEach { interval ->
            TangemMenu.Item(
                label = interval.title,
                isSelected = um.selected == interval,
                onClick = {
                    onDismiss()
                    um.onOptionClick(interval)
                },
            )
        }
    }
}