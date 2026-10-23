package com.tangem.feature.swap.mcp.ui

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
import com.tangem.feature.swap.mcp.model.ExchangeIntentButtonUM
import com.tangem.feature.swap.mcp.model.ExchangeIntentUM
import com.tangem.feature.swap.presentation.R

/**
 * An intent the wallet cannot offer: the status of the intent over the one action it carries.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=23363-122221)
 */
@Composable
internal fun ExchangeIntentStatusContent(state: ExchangeIntentUM.Status, modifier: Modifier = Modifier) {
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
private fun ExchangeIntentProcessedContentPreview() {
    TangemThemePreviewRedesign {
        ExchangeIntentStatusContent(
            state = ExchangeIntentUM.Status(
                variant = TangemModalStatus.Variant.Info,
                title = resourceReference(R.string.mcp_intent_processed_title),
                subtitle = resourceReference(
                    R.string.mcp_swap_intent_processed_subtitle,
                    wrappedList("42.00 USDT", "POL"),
                ),
                primaryButton = ExchangeIntentButtonUM(text = resourceReference(R.string.common_got_it), onClick = {}),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExchangeIntentCantReachContentPreview() {
    TangemThemePreviewRedesign {
        ExchangeIntentStatusContent(
            state = ExchangeIntentUM.Status(
                variant = TangemModalStatus.Variant.Warning,
                title = resourceReference(R.string.mcp_swap_intent_cant_reach_title),
                subtitle = null,
                primaryButton = ExchangeIntentButtonUM(
                    text = resourceReference(R.string.alert_button_try_again),
                    onClick = {},
                ),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}