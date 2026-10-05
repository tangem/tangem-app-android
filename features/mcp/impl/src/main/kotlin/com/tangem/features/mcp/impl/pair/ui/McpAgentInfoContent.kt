package com.tangem.features.mcp.impl.pair.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.PortfolioSelectRowV3
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.features.mcp.impl.R
import com.tangem.features.mcp.impl.pair.model.*

private val CardShape = RoundedCornerShape(24.dp)

@Composable
internal fun McpAgentInfoContent(state: McpPairUM, modifier: Modifier = Modifier) {
    val contentModifier = modifier.padding(bottom = 8.dp)
    when (state) {
        is McpPairUM.Loading -> AgentInfoSkeleton(contentModifier)
        is McpPairUM.AgentInfo -> AgentInfo(state = state, modifier = contentModifier)
        // Rendered by the success route, which replaces this one.
        is McpPairUM.Success -> Unit
    }
}

@Composable
private fun AgentInfo(state: McpPairUM.AgentInfo, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AgentCard(agent = state.agent)
        WalletCard(wallet = state.wallet)
        state.notificationsWarning?.let { NotificationsWarning(state = it) }
    }
}

@Composable
private fun AgentCard(agent: McpAgentUM, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        TangemRow(
            divider = true,
            contentLead = TangemRowContentLead.Start,
            verticalAlignment = TangemRowVerticalAlignment.Center,
            startSlot = { AgentAvatar() },
            titleSlot = { TangemRowText(text = agent.name, role = TangemRowTextRole.Title) },
            subtitleSlot = {
                agent.origin?.let { TangemRowText(text = it, role = TangemRowTextRole.Subtitle) }
            },
        )
        PermissionsBlock()
    }
}

@Composable
private fun AgentAvatar(modifier: Modifier = Modifier) {
    TangemAvatar(
        modifier = modifier,
        // The verification response carries no logo, so the agent is shown with the MCP glyph.
        state = TangemAvatar.UiState.Icon(
            imageVector = Icons.ic_grid_plus_20,
            appearance = TangemAvatar.Appearance.Neutral,
        ),
        size = TangemAvatar.Size.X40,
    )
}

@Composable
private fun PermissionsBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PermissionsGroup(caption = resourceReference(R.string.mcp_connection_request_agent_can)) {
            PermissionRow(
                isAllowed = true,
                text = resourceReference(R.string.mcp_connection_request_can_view_balance),
            )
            PermissionRow(
                isAllowed = true,
                text = resourceReference(R.string.wc_connection_reqeust_request_approval),
            )
        }
        PermissionsGroup(caption = resourceReference(R.string.wc_connection_reqeust_will_not)) {
            PermissionRow(
                isAllowed = false,
                text = resourceReference(R.string.wc_connection_reqeust_cant_sign),
            )
        }
    }
}

@Composable
private fun PermissionsGroup(caption: TextReference, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = caption.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        content()
    }
}

@Composable
private fun PermissionRow(isAllowed: Boolean, text: TextReference) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PermissionIcon(
            imageVector = if (isAllowed) Icons.ic_checkmark_16 else Icons.ic_cross_16,
            tint = if (isAllowed) {
                TangemTheme.colors3.icon.status.info
            } else {
                TangemTheme.colors3.icon.status.error
            },
            background = if (isAllowed) {
                TangemTheme.colors3.bg.status.infoSubtle
            } else {
                TangemTheme.colors3.bg.status.errorSubtle
            },
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun PermissionIcon(imageVector: ImageVector, tint: Color, background: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
        )
    }
}

@Composable
private fun WalletCard(wallet: PortfolioSelectUM, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        PortfolioSelectRowV3(state = wallet)
    }
}

@Composable
private fun NotificationsWarning(state: McpNotificationsWarningUM, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        modifier = modifier,
        variant = TangemMessageBanner.Variant.Warning,
        title = resourceReference(R.string.push_notification_settings_banner_title),
        description = resourceReference(R.string.mcp_connection_request_notifications_description),
        slotEnd = {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.ic_warning_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.warning,
            )
        },
        primaryButton = TangemMessageBanner.Button(
            text = resourceReference(R.string.common_open_settings_button_title),
            onClick = state.onOpenSettingsClick,
        ),
    )
}

@Composable
private fun AgentInfoSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            TangemRow(
                divider = true,
                contentLead = TangemRowContentLead.Start,
                verticalAlignment = TangemRowVerticalAlignment.Center,
                startSlot = {
                    TangemAvatar(
                        state = TangemAvatar.UiState.Shimmer(TangemAvatar.UiState.Shimmer.Shape.RoundedSquare),
                        size = TangemAvatar.Size.X40,
                    )
                },
                titleSlot = {
                    TangemShimmer(
                        modifier = Modifier.width(120.dp),
                        style = TangemTheme.typography3.body.medium,
                    )
                },
                subtitleSlot = {
                    TangemShimmer(
                        modifier = Modifier.width(168.dp),
                        style = TangemTheme.typography3.caption.medium,
                    )
                },
            )
            PermissionsBlock()
        }
        Card {
            TangemRow(
                verticalAlignment = TangemRowVerticalAlignment.Center,
                titleSlot = {
                    TangemRowText(text = resourceReference(R.string.wc_common_wallet), role = TangemRowTextRole.Title)
                },
                valueSlot = {
                    TangemShimmer(
                        modifier = Modifier.width(96.dp),
                        style = TangemTheme.typography3.body.medium,
                    )
                },
            )
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.tertiary,
        shape = CardShape,
    ) {
        Column(content = content)
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpAgentInfoContentPreview() {
    PreviewContainer { McpAgentInfoContent(state = McpAgentInfoPreviewData.content) }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpAgentInfoContentWithWarningPreview() {
    PreviewContainer { McpAgentInfoContent(state = McpAgentInfoPreviewData.contentWithWarning) }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpAgentInfoContentLoadingPreview() {
    PreviewContainer { McpAgentInfoContent(state = McpPairUM.Loading) }
}

@Composable
private fun PreviewContainer(content: @Composable () -> Unit) {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary),
            content = { content() },
        )
    }
}

private object McpAgentInfoPreviewData {

    val content = McpPairUM.AgentInfo(
        agent = McpAgentUM(name = stringReference("Claude Agent"), origin = stringReference("mcp.claude.com")),
        wallet = PortfolioSelectUM(
            icon = null,
            name = stringReference("My wallet"),
            isAccountMode = false,
            isMultiChoice = true,
            onClick = {},
        ),
        notificationsWarning = null,
        connectButton = McpConnectButtonUM(isEnabled = true, isLoading = false, onClick = {}),
        onDecline = {},
    )

    val contentWithWarning = content.copy(
        notificationsWarning = McpNotificationsWarningUM(onOpenSettingsClick = {}),
        connectButton = content.connectButton.copy(isEnabled = false),
    )
}