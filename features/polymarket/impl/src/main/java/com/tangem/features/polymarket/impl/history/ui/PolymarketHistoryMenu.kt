package com.tangem.features.polymarket.impl.history.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.contextmenu.TangemContextMenuItem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_dots_vertical_20

@Composable
internal fun PolymarketHistoryMenu(onDownloadClick: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
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
        ) {
            TangemContextMenuItem(
                title = resourceReference(R.string.prediction_history_download_csv),
                icon = Icons.ic_arrow_down_24,
                onClick = {
                    isExpanded = false
                    onDownloadClick()
                },
            )
        }
    }
}