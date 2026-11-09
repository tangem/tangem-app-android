package com.tangem.features.polymarket.impl.fasttrading.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tangem.core.ui.res.generated.icons.ic_dots_horizontal_20
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM.RevokeSubject
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun PolymarketFastTradingKeyActionsMenu(
    actions: ImmutableList<FastTradingKeyActionUM>,
    isEnabled: Boolean,
    onActionClick: (FastTradingKeyActionUM) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }
    val menuOffset = with(LocalDensity.current) { DpOffset(x = 0.dp, y = -anchorHeightPx.toDp()) }
    LaunchedEffect(isEnabled) {
        if (!isEnabled) isExpanded = false
    }
    Box(modifier = modifier.onSizeChanged { anchorHeightPx = it.height }) {
        TangemButton(
            variant = TangemButton.Variant.Ghost,
            size = TangemButton.Size.X9,
            iconStart = TangemIconUM.Icon(Icons.ic_dots_horizontal_20),
            isEnabled = isEnabled,
            contentDescription = stringResourceSafe(R.string.common_more),
            onClick = { isExpanded = true },
        )
        TangemContextMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.hazeEffectTangem { blurRadius = 10.dp },
            offset = menuOffset,
        ) {
            actions.forEach { action ->
                KeyActionMenuItem(
                    action = action,
                    onClick = {
                        isExpanded = false
                        onActionClick(action)
                    },
                )
            }
        }
    }
}

@Composable
private fun KeyActionMenuItem(action: FastTradingKeyActionUM, onClick: () -> Unit) {
    when (action) {
        is FastTradingKeyActionUM.Revoke -> TangemContextMenuItem(
            title = resourceReference(R.string.prediction_session_key_revoke),
            isDestructive = true,
            onClick = onClick,
        )
    }
}

@Preview(name = "Key actions menu", widthDp = 240)
@Preview(name = "Key actions menu dark", widthDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun KeyActionMenuItemPreview() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            KeyActionMenuItem(
                action = FastTradingKeyActionUM.Revoke("0x1", RevokeSubject.UnknownSigner),
                onClick = {},
            )
        }
    }
}