package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.connections.entity.McpSessionInfoUM
import com.tangem.features.walletconnect.impl.R

/** MVP details of one MCP session: the session model dumped as text plus the revoke action. */
@Composable
internal fun McpSessionInfoBS(state: McpSessionInfoUM) {
    TangemModalBottomSheet<McpSessionInfoUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = state,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemModalBottomSheetTitle(
                title = stringReference("Connected agent"),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = state.onDismiss,
            )
        },
        content = { McpSessionInfoContent(state = state, modifier = Modifier.padding(horizontal = 16.dp)) },
    )
}

@Composable
private fun McpSessionInfoContent(state: McpSessionInfoUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // The sheet itself scrolls vertically, the dump only has to handle its own long lines
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(TangemTheme.colors.background.action),
        ) {
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    text = state.debugText,
                    style = TangemTheme.typography.caption2.copy(fontFamily = FontFamily.Monospace),
                    color = TangemTheme.colors.text.primary1,
                    softWrap = false,
                )
            }
        }
        SecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            text = "Revoke access",
            showProgress = state.isRevoking,
            enabled = state.debugText.isNotEmpty(),
            onClick = state.onRevokeClick,
        )
    }
}

// A device is required: the modal sheet sizes itself from the window height, and a wrap-content
// preview leaves that height unbounded, so nothing gets laid out.
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpSessionInfoBSPreview() {
    TangemThemePreview {
        McpSessionInfoBS(state = previewState)
    }
}

/** Renders without the modal sheet machinery, so it survives a broken preview render process. */
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpSessionInfoContentPreview() {
    TangemThemePreview {
        McpSessionInfoContent(
            state = previewState,
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(16.dp),
        )
    }
}

private val previewState = McpSessionInfoUM(
    debugText = """
        McpSessionResponse(
          id=b3f1c0d2-9a4e-4c11-8f2a-6d5e7c8b9a01,
          clientId=mcp-client-42,
          clientOrigin=claude.ai,
          clientName=Claude Code,
          scopes=[
            wallet:read,
            wallet:sign
          ],
          clientIp=203.0.113.7,
          clientCountry=DE,
          clientUserAgent=claude-code/1.0.0
        )
    """.trimIndent(),
    isRevoking = false,
    onRevokeClick = {},
    onDismiss = {},
)