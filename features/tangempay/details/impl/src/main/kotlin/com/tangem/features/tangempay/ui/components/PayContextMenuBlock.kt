package com.tangem.features.tangempay.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDropDownItemUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

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
            PayContextMenuItem(item = item, onMenuDismiss = onMenuDismiss)
        }
    }
}

@Composable
private fun PayContextMenuItem(item: TangemPayDropDownItemUM, onMenuDismiss: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .conditional(
                condition = item.isEnabled,
                modifier = {
                    clickableSingle(
                        onClick = {
                            item.onClick()
                            onMenuDismiss()
                        },
                    )
                },
            )
            .padding(vertical = TangemTheme.dimens2.x3, horizontal = TangemTheme.dimens2.x4),
    ) {
        TangemIcon(
            modifier = Modifier.size(TangemTheme.dimens2.x5),
            tangemIconUM = item.icon,
        )
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x0_5)) {
            Text(
                text = item.title.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = when {
                    !item.isEnabled -> TangemTheme.colors3.text.tertiary
                    item.titleColor != null -> item.titleColor.invoke()
                    else -> TangemTheme.colors3.text.primary
                },
                maxLines = 1,
            )
            item.subtitle?.let { subtitle ->
                Text(
                    text = subtitle.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.tertiary,
                    maxLines = 2,
                )
            }
        }
    }
}

// region Preview

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        Column {
            persistentListOf(
                previewReplaceItem(),
                previewCloseItem(),
            ).fastForEach { item ->
                PayContextMenuItem(item = item, onMenuDismiss = {})
            }
        }
    }
}

private fun previewReplaceItem() = TangemPayDropDownItemUM(
    title = TextReference.Res(R.string.tangempay_card_details_reissue_card),
    onClick = {},
    icon = TangemIconUM.Icon(
        iconRes = CoreUiR.drawable.ic_refresh_24,
        tintReference = { TangemTheme.colors3.icon.primary },
    ),
)

private fun previewCloseItem() = TangemPayDropDownItemUM(
    title = TextReference.Res(R.string.tangem_pay_close_card_popup_primary_button_title),
    onClick = {},
    icon = TangemIconUM.Icon(
        iconRes = CoreUiR.drawable.ic_trash_24,
        tintReference = { TangemTheme.colors3.icon.tertiary },
    ),
    subtitle = TextReference.Res(R.string.tangem_pay_close_card_disabled_last_card),
    isEnabled = false,
)
// endregion