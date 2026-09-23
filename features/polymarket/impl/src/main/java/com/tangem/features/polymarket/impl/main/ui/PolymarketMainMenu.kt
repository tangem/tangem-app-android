package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.contextmenu.TangemContextMenuItem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_24
import com.tangem.core.ui.res.generated.icons.ic_dots_vertical_20

@Composable
internal fun PolymarketMainMenu(onHistoryClick: () -> Unit, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }
    val menuOffset = with(LocalDensity.current) { DpOffset(x = 0.dp, y = -anchorHeightPx.toDp()) }
    Box(modifier = modifier.onSizeChanged { anchorHeightPx = it.height }) {
        TangemButton(
            variant = TangemButton.Variant.Material,
            size = TangemButton.Size.X11,
            iconStart = TangemIconUM.Icon(Icons.ic_dots_vertical_20),
            contentDescription = stringResourceSafe(R.string.common_more),
            onClick = { isExpanded = true },
        )
        TangemContextMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.hazeEffectTangem { blurRadius = 10.dp },
            offset = menuOffset,
        ) {
            HistoryMenuItem(onClick = {
                isExpanded = false
                onHistoryClick()
            })
        }
    }
}

@Composable
private fun HistoryMenuItem(onClick: () -> Unit) {
    TangemContextMenuItem(
        title = resourceReference(R.string.prediction_history_title),
        icon = Icons.ic_clock_24,
        onClick = onClick,
    )
}

@Preview(name = "History menu", widthDp = 240)
@Preview(name = "History menu dark", widthDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "History menu large text", widthDp = 240, fontScale = 1.5f)
@Composable
private fun PolymarketMainMenuPreview() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            HistoryMenuItem(onClick = {})
        }
    }
}