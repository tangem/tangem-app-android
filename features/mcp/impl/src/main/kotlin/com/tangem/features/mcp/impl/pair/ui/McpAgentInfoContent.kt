package com.tangem.features.mcp.impl.pair.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.common.ui.account.PortfolioSelectRow
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.mcp.impl.R
import com.tangem.features.mcp.impl.pair.model.McpAgentUM
import com.tangem.features.mcp.impl.pair.model.McpConnectButtonUM
import com.tangem.features.mcp.impl.pair.model.McpPairTexts
import com.tangem.features.mcp.impl.pair.model.McpPairUM
import com.tangem.features.mcp.impl.pair.model.McpPushNotificationsWarningUM

private const val WARNING_BACKGROUND_ALPHA = 0.1F

@Composable
internal fun McpAgentInfoContent(state: McpPairUM, modifier: Modifier = Modifier) {
    when (state) {
        is McpPairUM.Loading -> LoadingContent(modifier)
        is McpPairUM.Content -> AgentInfo(state = state, modifier = modifier)
    }
}

@Composable
private fun AgentInfo(state: McpPairUM.Content, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val blocksModifier = Modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(color = TangemTheme.colors.background.action)
            .fillMaxWidth()
        AgentBlock(modifier = blocksModifier, state = state)
        PortfolioBlock(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing14)
                .then(blocksModifier),
            portfolioSelectRow = state.portfolioSelectRow,
        )
        state.pushNotificationsWarning?.let { warning ->
            PushNotificationsWarning(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing14),
                state = warning,
            )
        }
    }
}

@Composable
private fun PushNotificationsWarning(state: McpPushNotificationsWarningUM, modifier: Modifier = Modifier) {
    Notification(
        modifier = modifier.fillMaxWidth(),
        containerColor = TangemTheme.colors.icon.warning.copy(alpha = WARNING_BACKGROUND_ALPHA),
        config = NotificationConfig(
            title = resourceReference(R.string.push_notification_settings_banner_title),
            subtitle = McpPairTexts.pushNotificationsWarning,
            iconResId = R.drawable.ic_alert_circle_24,
            iconTint = NotificationConfig.IconTint.Warning,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.common_open_settings_button_title),
                onClick = state.onOpenSettingsClick,
            ),
        ),
    )
}

@Composable
private fun AgentBlock(state: McpPairUM.Content, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        AgentItem(agent = state.agent)
        HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors.stroke.primary)
        VerificationDump(
            text = state.agent.verificationText,
            modifier = Modifier.padding(TangemTheme.dimens.spacing12),
        )
    }
}

@Composable
private fun AgentItem(agent: McpAgentUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        val iconModifier = Modifier
            .size(TangemTheme.dimens.size48)
            .clip(RoundedCornerShape(TangemTheme.dimens.radius8))
        if (agent.iconUrl == null) {
            Box(modifier = iconModifier.background(TangemTheme.colors.background.tertiary))
        } else {
            AsyncImage(modifier = iconModifier, model = agent.iconUrl, contentDescription = null)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        ) {
            Text(
                text = agent.name.resolveReference(),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.h3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = agent.subtitle.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The verification response has no contract to lay out yet, so it is dumped as text. */
@Composable
private fun VerificationDump(text: String, modifier: Modifier = Modifier) {
    // The route already scrolls vertically, the dump only has to handle its own long lines
    SelectionContainer(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Text(
            text = text,
            style = TangemTheme.typography.caption2.copy(fontFamily = FontFamily.Monospace),
            color = TangemTheme.colors.text.primary1,
            softWrap = false,
        )
    }
}

@Composable
private fun PortfolioBlock(portfolioSelectRow: PortfolioSelectUM, modifier: Modifier = Modifier) {
    PortfolioSelectRow(modifier = modifier, state = portfolioSelectRow) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(id = R.drawable.ic_wallet_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        SpacerW4()
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens.spacing32),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TangemTheme.colors.icon.accent)
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpAgentInfoContentPreview() {
    TangemThemePreview {
        McpAgentInfoContent(
            state = McpAgentInfoPreviewData.content,
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(TangemTheme.dimens.spacing16),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpAgentInfoContentWithPushWarningPreview() {
    TangemThemePreview {
        McpAgentInfoContent(
            state = McpAgentInfoPreviewData.contentWithPushWarning,
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(TangemTheme.dimens.spacing16),
        )
    }
}

private object McpAgentInfoPreviewData {

    val content = McpPairUM.Content(
        agent = McpAgentUM(
            name = stringReference("Claude Agent"),
            subtitle = stringReference("mcp.claude.com"),
            iconUrl = null,
            verificationText = """
                McpDeviceVerification(
                  userCode=LWHN-KMFS,
                  clientOrigin=claude.ai,
                  clientName=Claude Code,
                  scopes=[
                    wallet:read,
                    wallet:sign
                  ],
                  expiresIn=600
                )
            """.trimIndent(),
        ),
        portfolioSelectRow = PortfolioSelectUM(
            icon = null,
            name = stringReference("My wallet"),
            isAccountMode = false,
            isMultiChoice = true,
            onClick = {},
        ),
        pushNotificationsWarning = null,
        connectButton = McpConnectButtonUM(isEnabled = true, isLoading = false, onClick = {}),
        onDecline = {},
    )

    val contentWithPushWarning = content.copy(
        pushNotificationsWarning = McpPushNotificationsWarningUM(onOpenSettingsClick = {}),
        connectButton = content.connectButton.copy(isEnabled = false),
    )
}