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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
        style = TangemTheme.typography2.headingSemibold17,
        color = if (organizeMenuUM.isSortedByBalance) {
            TangemTheme.colors2.text.status.disabled
        } else {
            TangemTheme.colors2.text.neutral.primary
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
            .padding(vertical = TangemTheme.dimens2.x5, horizontal = TangemTheme.dimens2.x4),
    )

    HorizontalDivider(
        thickness = 0.5.dp,
        color = TangemTheme.colors2.border.neutral.quaternary,
    )
}

@Composable
private fun GroupTokensMenuSection(organizeMenuUM: OrganizeTokensUM.OrganizeMenuUM, onDropdownDismiss: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(238.dp)
            .clickableSingle(
                onClick = {
                    organizeMenuUM.onGroupClick()
                    onDropdownDismiss()
                },
            )
            .padding(vertical = TangemTheme.dimens2.x5, horizontal = TangemTheme.dimens2.x4),
    ) {
        Text(
            text = stringResourceSafe(R.string.organize_tokens_group),
            style = TangemTheme.typography2.headingSemibold17,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
        )
        if (organizeMenuUM.isGrouped) {
            Box(
                modifier = Modifier
                    .padding(TangemTheme.dimens2.x0_5)
                    .size(TangemTheme.dimens2.x5)
                    .background(
                        color = TangemTheme.colors2.graphic.neutral.primary,
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    painter = rememberVectorPainter(
                        ImageVector.vectorResource(R.drawable.ic_check_default_24),
                    ),
                    contentDescription = null,
                    tint = TangemTheme.colors2.graphic.neutral.primaryInverted,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(TangemTheme.dimens2.x0_5)
                        .size(TangemTheme.dimens2.x4),
                )
            }
        }
    }
}