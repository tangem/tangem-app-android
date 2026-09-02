package com.tangem.features.collectibles.impl.main.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_dots_vertical_20
import com.tangem.features.collectibles.impl.main.ui.preview.CollectiblesMainPreviewData
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM

private val MenuWidth = 238.dp

@Composable
internal fun CollectiblesMainMenu(menu: CollectiblesMainUM.MenuUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        TangemButton(
            variant = TangemButton.Variant.Material,
            size = TangemButton.Size.X11,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_dots_vertical_20),
            onClick = menu.onMenuClick,
        )
        TangemContextMenu( // TODO align with design
            expanded = menu.isShown,
            onDismissRequest = menu.onDismiss,
            modifier = Modifier.width(MenuWidth),
        ) {
            menu.items.fastForEach { item ->
                if (item.hasDividerAbove) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = TangemTheme.colors3.border.primary,
                    )
                }
                CollectiblesMenuItem(
                    item = item,
                    onClick = { item.onClick() },
                )
            }
        }
    }
}

@Composable
private fun CollectiblesMenuItem(
    item: CollectiblesMainUM.MenuItemUM,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemIcon(
            tangemIconUM = TangemIconUM.Icon(imageVector = item.icon),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = item.title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(widthDp = 360, heightDp = 400, showBackground = true)
@Preview(widthDp = 360, heightDp = 400, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesMainMenuPreview() {
    TangemThemePreviewRedesign {
        CollectiblesMainMenu(menu = CollectiblesMainPreviewData.menuShown.menu)
    }
}