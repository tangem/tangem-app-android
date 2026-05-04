package com.tangem.features.feed.ui.market.list.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.contextmenu.TangemContextMenuCheckboxItem
import com.tangem.features.feed.model.market.list.state.SortByMenuUM
import com.tangem.features.feed.model.market.list.state.SortByTypeUM

@Composable
internal fun SortByMenu(
    sortMenuUM: SortByMenuUM,
    showDropdownMenu: Boolean,
    onDropdownDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemContextMenu(
        expanded = showDropdownMenu,
        onDismissRequest = onDropdownDismiss,
        offset = DpOffset.Zero,
        modifier = modifier,
    ) {
        SortByTypeUM.entries.fastForEach { sortType ->
            TangemContextMenuCheckboxItem(
                title = sortType.text,
                isChecked = sortMenuUM.selectedOption == sortType,
                onClick = {
                    sortMenuUM.onOptionClicked(sortType)
                    onDropdownDismiss()
                },
            )
        }
    }
}