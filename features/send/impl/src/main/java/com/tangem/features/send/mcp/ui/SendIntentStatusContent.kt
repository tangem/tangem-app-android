package com.tangem.features.send.mcp.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
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
                title = stringReference("This intent has already been processed"),
                subtitle = stringReference("Send 42.00 USDT"),
                primaryButton = SendIntentButtonUM(text = stringReference("Got it"), onClick = {}),
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
                title = stringReference("Can’t reach intent"),
                subtitle = null,
                primaryButton = SendIntentButtonUM(text = stringReference("Try again"), onClick = {}),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}