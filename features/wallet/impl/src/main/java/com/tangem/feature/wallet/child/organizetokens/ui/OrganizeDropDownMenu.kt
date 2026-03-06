package com.tangem.feature.wallet.child.organizetokens.ui

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.contextmenu.TangemContextMenuCheckboxItem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensUM
import com.tangem.feature.wallet.impl.R

@Composable
internal fun OrganizeDropDownMenu(
    organizeMenuUM: OrganizeTokensUM.OrganizeMenuUM,
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
        TangemContextMenuCheckboxItem(
            title = TextReference.Res(R.string.organize_tokens_sort_by_balance),
            isChecked = organizeMenuUM.isSortedByBalance,
            onClick = organizeMenuUM.onSortClick,
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = TangemTheme.colors2.border.neutral.quaternary,
        )
        TangemContextMenuCheckboxItem(
            title = TextReference.Res(R.string.organize_tokens_group),
            isChecked = organizeMenuUM.isGrouped,
            onClick = organizeMenuUM.onGroupClick,
        )
    }
}