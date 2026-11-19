package com.tangem.features.feed.crypto.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.features.feed.crypto.model.list.MarketPulseInterval
import com.tangem.features.feed.crypto.ui.state.MarketPulseIntervalUM

@Composable
internal fun MarketPulseIntervalMenu(
    um: MarketPulseIntervalUM,
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
        MarketPulseInterval.entries.fastForEach { interval ->
            TangemMenu.Item(
                label = interval.intervalTitle(),
                isSelected = um.selected == interval,
                onClick = {
                    onDismiss()
                    um.onOptionClick(interval)
                },
            )
        }
    }
}