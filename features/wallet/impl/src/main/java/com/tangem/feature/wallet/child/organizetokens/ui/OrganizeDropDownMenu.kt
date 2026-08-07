package com.tangem.feature.wallet.child.organizetokens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.stringResourceSafe
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
        Menu(
            organizeMenuUM = organizeMenuUM,
            onDropdownDismiss = onDropdownDismiss,
        )
    }
}

@Composable
private fun Menu(
    onDropdownDismiss: () -> Unit,
    organizeMenuUM: OrganizeTokensUM.OrganizeMenuUM,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SortByBalanceMenuSection(
            organizeMenuUM = organizeMenuUM,
            onDropdownDismiss = onDropdownDismiss,
        )
        GroupTokensMenuSection(
            organizeMenuUM = organizeMenuUM,
            onDropdownDismiss = onDropdownDismiss,
        )
    }
}

@Composable
private fun SortByBalanceMenuSection(organizeMenuUM: OrganizeTokensUM.OrganizeMenuUM, onDropdownDismiss: () -> Unit) {
    Text(
        text = stringResourceSafe(R.string.organize_tokens_sort_by_balance),
        style = TangemTheme.typography3.body.medium,
        color = if (organizeMenuUM.isSortedByBalance) {
            TangemTheme.colors3.text.tertiary
        } else {
            TangemTheme.colors3.text.primary
        },
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(238.dp)
            .clickableSingle(
                onClick = {
                    organizeMenuUM.onSortClick()
                    onDropdownDismiss()
                },
                enabled = !organizeMenuUM.isSortedByBalance,
            )
            .padding(vertical = 10.dp, horizontal = 16.dp),
    )

    HorizontalDivider(
        thickness = 0.5.dp,
        color = TangemTheme.colors3.border.tertiary,
    )
}

@Composable
private fun GroupTokensMenuSection(organizeMenuUM: OrganizeTokensUM.OrganizeMenuUM, onDropdownDismiss: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(238.dp)
            .clickableSingle(
                onClick = {
                    organizeMenuUM.onGroupClick()
                    onDropdownDismiss()
                },
            )
            .padding(vertical = 10.dp, horizontal = 16.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.organize_tokens_group),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
        )
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (organizeMenuUM.isGrouped) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(20.dp)
                        .background(
                            color = TangemTheme.colors3.icon.primary,
                            shape = CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_check_default_24),
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.inverse,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(2.dp)
                            .size(16.dp),
                    )
                }
            }
        }
    }
}