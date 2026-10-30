package com.tangem.features.send.mcp.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.send.impl.R
import com.tangem.features.send.mcp.model.SendIntentButtonUM
import com.tangem.features.send.mcp.model.SendIntentUM

/**
 * An intent the wallet cannot offer: the status of the intent over the one action it carries.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=23363-122221)
 */
@Composable
internal fun SendIntentStatusContent(state: SendIntentUM.Status, modifier: Modifier = Modifier) {
    TangemModalStatus(
        modifier = modifier,
        variant = state.variant,
        title = state.title,
        description = state.subtitle,
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendIntentProcessedContentPreview() {
    TangemThemePreviewRedesign {
        SendIntentStatusContent(
            state = SendIntentUM.Status(
                variant = TangemModalStatus.Variant.Info,
                title = resourceReference(R.string.mcp_intent_processed_title),
                subtitle = resourceReference(R.string.mcp_send_intent_processed_subtitle, wrappedList("42.00 USDT")),
                primaryButton = SendIntentButtonUM(text = resourceReference(R.string.common_got_it), onClick = {}),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendIntentCantReachContentPreview() {
    TangemThemePreviewRedesign {
        SendIntentStatusContent(
            state = SendIntentUM.Status(
                variant = TangemModalStatus.Variant.Warning,
                title = resourceReference(R.string.mcp_send_intent_cant_reach_title),
                subtitle = null,
                primaryButton = SendIntentButtonUM(
                    text = resourceReference(R.string.alert_button_try_again),
                    onClick = {},
                ),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}