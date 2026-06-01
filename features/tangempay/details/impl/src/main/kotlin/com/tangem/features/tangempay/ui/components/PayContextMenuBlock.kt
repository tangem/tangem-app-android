package com.tangem.features.tangempay.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.entity.TangemPayDropDownItemUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun PayContextMenuBlock(
    items: ImmutableList<TangemPayDropDownItemUM>,
    isDropdownMenuShown: Boolean,
    onMenuDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemContextMenu(
        expanded = isDropdownMenuShown,
        onDismissRequest = onMenuDismiss,
        offset = DpOffset.Zero,
        modifier = modifier,
    ) {
        items.fastForEach { item ->
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
                    modifier = Modifier
                        .clickableSingle(
                            onClick = {
                                item.onClick()
                                onMenuDismiss()
                            },
                        )
                        .padding(vertical = TangemTheme.dimens2.x3, horizontal = TangemTheme.dimens2.x4),
                ) {
                    TangemIcon(
                        modifier = Modifier.size(TangemTheme.dimens2.x5),
                        tangemIconUM = item.icon,
                    )
                    Text(
                        text = item.title.resolveReference(),
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.primary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}