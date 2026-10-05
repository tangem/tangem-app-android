package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.PortfolioSelectRowV3
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.ui.components.bottomsheets.LocalBottomSheetContentScrollable
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.walletconnect.connections.entity.McpSessionInfoUM
import com.tangem.features.walletconnect.impl.R

private val CardShape = RoundedCornerShape(24.dp)

@Composable
internal fun McpSessionInfoBS(state: McpSessionInfoUM) {
    TangemBottomSheet<McpSessionInfoUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = state,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.mcp_session_title),
                subtitle = stringReference(state.connectedAt),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = state.onDismiss,
            )
        },
        content = { McpSessionInfoContent(state = it) },
        footer = {
            TangemFooter(
                modifier = Modifier.fillMaxWidth(),
                windowInsets = WindowInsets(0),
                fadeEnabled = false,
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.mcp_session_revoke_access),
                    isLoading = state.isRevoking,
                    onClick = state.onRevokeClick,
                )
            }
        },
    )
}

@Composable
private fun McpSessionInfoContent(state: McpSessionInfoUM, modifier: Modifier = Modifier) {
    // Two rows never scroll, and a sheet that is told otherwise reserves a fade gradient's worth of
    // empty space on top of the footer it already makes room for.
    val contentScrollable = LocalBottomSheetContentScrollable.current
    LaunchedEffect(Unit) { contentScrollable?.value = false }

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        TangemSurface(
            modifier = Modifier.fillMaxWidth(),
            color = TangemTheme.colors3.bg.tertiary,
            shape = CardShape,
        ) {
            Column {
                AgentRow(state = state)
                PortfolioSelectRowV3(state = state.wallet)
            }
        }
        Spacer(modifier = Modifier.height(LocalTangemBottomSheetContentBottomInset.current))
    }
}

@Composable
private fun AgentRow(state: McpSessionInfoUM) {
    TangemRow(
        divider = true,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { McpAgentAvatar(iconUrl = state.agentIconUrl) },
        titleSlot = { TangemRowText(text = state.agentName, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = state.agentOrigin, role = TangemRowTextRole.Subtitle) },
    )
}

// A device is required: the modal sheet sizes itself from the window height, and a wrap-content
// preview leaves that height unbounded, so nothing gets laid out.
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpSessionInfoBSPreview() {
    TangemThemePreviewRedesign { McpSessionInfoBS(state = previewState) }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpSessionInfoContentPreview() {
    TangemThemePreviewRedesign { McpSessionInfoContent(state = previewState) }
}

private val previewState = McpSessionInfoUM(
    agentName = "Claude Agent",
    agentOrigin = "mcp.claude.com",
    agentIconUrl = null,
    connectedAt = "Jan 20, 2026, 9:24 PM",
    wallet = PortfolioSelectUM(
        icon = null,
        name = stringReference("My wallet"),
        isAccountMode = false,
        isMultiChoice = false,
        onClick = {},
    ),
    isRevoking = false,
    onRevokeClick = {},
    onDismiss = {},
)