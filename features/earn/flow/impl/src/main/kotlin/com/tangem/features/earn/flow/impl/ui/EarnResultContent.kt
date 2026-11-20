@file:Suppress("MagicNumber")

package com.tangem.features.earn.flow.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.GroupEntry
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonStack
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_20
import com.tangem.core.ui.res.generated.icons.ic_dot_16_filled
import com.tangem.core.ui.res.generated.icons.ic_globe_20
import com.tangem.core.ui.res.generated.icons.ic_share_android_20
import com.tangem.features.earn.flow.impl.ui.state.EarnResultUM
import com.tangem.features.earn.product.api.session.EarnResultAsset

@Composable
internal fun EarnResultContent(state: EarnResultUM, modifier: Modifier = Modifier) {
    TangemStatusScreen(
        variant = state.variant,
        title = state.title,
        description = state.subtitle,
        modifier = modifier,
        asset = { Asset(state) },
        extraBottom = state.chip?.let { chip ->
            {
                TangemBadge(
                    text = chip,
                    status = TangemBadge.Status.Success,
                    size = TangemBadge.Size.X6,
                    iconStart = TangemIconUM.Icon(Icons.ic_dot_16_filled),
                )
            }
        },
        details = if (state.rows.isNotEmpty()) {
            { EarnRows(rows = state.rows, style = EarnRowsStyle.Details) }
        } else {
            null
        },
        footer = {
            val explorer = state.explorer
            if (explorer != null) ExplorerFooter(explorer = explorer, state = state) else ButtonsFooter(state)
        },
    )
}

@Composable
private fun Asset(state: EarnResultUM) {
    when (val asset = state.asset) {
        EarnResultAsset.Logos -> EarnLogos(icons = state.icons, size = 56.dp, overlap = 8.dp)
        is EarnResultAsset.Status -> TangemStatusIcon(kind = asset.kind, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun ButtonsFooter(state: EarnResultUM) {
    TangemFooter(
        modifier = Modifier.padding(top = 16.dp),
        windowInsets = WindowInsets(0),
        fadeEnabled = false,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.secondary?.let { EarnFlowButton(button = it, modifier = Modifier.weight(1f)) }
            state.primary?.let { EarnFlowButton(button = it, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ExplorerFooter(explorer: EarnResultUM.ExplorerUM, state: EarnResultUM) {
    TangemFooter(
        modifier = Modifier.padding(top = 16.dp),
        windowInsets = WindowInsets(0),
        fadeEnabled = false,
        buttonStart = {
            TangemButtonStack {
                TangemButton.GroupEntry(
                    iconUM = TangemIconUM.Icon(Icons.ic_globe_20),
                    onClick = explorer.onExploreClick,
                )
                TangemButton.GroupEntry(
                    iconUM = TangemIconUM.Icon(Icons.ic_share_android_20),
                    onClick = explorer.onShareClick,
                )
            }
        },
        buttonEnd = {
            TangemButton(
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X11,
                iconStart = TangemIconUM.Icon(Icons.ic_checkmark_20),
                onClick = state.primary?.onClick ?: state.onClose,
            )
        },
    )
}