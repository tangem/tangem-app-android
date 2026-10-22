package com.tangem.features.send.mcp.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.send.mcp.model.SendIntentAgentUM
import com.tangem.features.send.mcp.model.SendIntentButtonUM
import com.tangem.features.send.mcp.model.SendIntentUM

/**
 * What an agent asks the owner to do, before anything is opened: the currency over the request, with the agent
 * that asks below.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=23363-122221)
 */
@Composable
internal fun SendIntentOfferContent(state: SendIntentUM, modifier: Modifier = Modifier) {
    TangemModalStatus(
        modifier = modifier,
        variant = TangemModalStatus.Variant.Info,
        asset = { TangemTokenIcon(state = state.token, size = TangemTokenIcon.Size.X56) },
        title = state.title,
        details = { SendIntentAgentDetails(agent = state.agent, description = state.description) },
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendIntentOfferContentPreview() {
    TangemThemePreviewRedesign {
        SendIntentOfferContent(
            state = SendIntentUM(
                token = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
                title = stringReference("mcp.claude.com Agent wants to send 42.00 USDT"),
                agent = SendIntentAgentUM.Verified(iconUrl = null, label = stringReference("mcp.claude.com")),
                description = stringReference(
                    "The agent is asking to send from your account. You’ll review the details next.",
                ),
                primaryButton = SendIntentButtonUM(text = stringReference("Proceed"), onClick = {}),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}