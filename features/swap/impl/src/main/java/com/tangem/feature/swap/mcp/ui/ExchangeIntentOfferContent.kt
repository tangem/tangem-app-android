package com.tangem.feature.swap.mcp.ui

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
import com.tangem.feature.swap.mcp.model.ExchangeIntentAgentUM
import com.tangem.feature.swap.mcp.model.ExchangeIntentButtonUM
import com.tangem.feature.swap.mcp.model.ExchangeIntentUM

/**
 * What an agent asks the owner to do, before anything is opened: the two currencies over the request, with the agent
 * that asks below.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=25973-248232)
 */
@Composable
internal fun ExchangeIntentOfferContent(state: ExchangeIntentUM, modifier: Modifier = Modifier) {
    TangemModalStatus(
        modifier = modifier,
        variant = TangemModalStatus.Variant.Info,
        asset = { ExchangeIntentTokenPair(from = state.fromToken, to = state.toToken) },
        title = state.title,
        details = { ExchangeIntentAgentDetails(agent = state.agent, description = state.description) },
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExchangeIntentOfferContentPreview() {
    TangemThemePreviewRedesign {
        ExchangeIntentOfferContent(
            state = ExchangeIntentUM(
                fromToken = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
                toToken = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
                title = stringReference("mcp.claude.com Agent wants to swap 42.00 USDT to POL"),
                agent = ExchangeIntentAgentUM.Verified(iconUrl = null, label = stringReference("mcp.claude.com")),
                description = stringReference(
                    "The agent is asking to swap from your account. You’ll review the details next.",
                ),
                primaryButton = ExchangeIntentButtonUM(text = stringReference("Proceed"), onClick = {}),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}