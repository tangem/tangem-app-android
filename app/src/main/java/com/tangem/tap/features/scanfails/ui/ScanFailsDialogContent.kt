package com.tangem.tap.features.scanfails.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tangem.core.ui.extensions.LocalUserInteractionTracker
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.trackUserInteraction
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.wallet.R

@Composable
internal fun ScanFailsDialogContent(state: ScanFailsUM) {
    val userInteractionTracker = LocalUserInteractionTracker.current

    Dialog(
        onDismissRequest = state.onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true),
    ) {
        Column(
            modifier = Modifier
                .trackUserInteraction(userInteractionTracker)
                .background(
                    color = TangemTheme.colors.background.primary,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(vertical = 16.dp, horizontal = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.common_warning),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.h3,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResourceSafe(R.string.alert_troubleshooting_scan_card_title),
                color = TangemTheme.colors.text.secondary,
                style = TangemTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

            DialogTextButton(
                text = stringResourceSafe(R.string.alert_button_how_to_scan),
                onClick = state.onHowToScan,
                color = TangemTheme.colors.text.accent,
                modifier = Modifier.testTag(ScanFailsDialogTestTags.HOW_TO_SCAN_BUTTON),
            )

            DialogTextButton(
                text = stringResourceSafe(R.string.alert_button_request_support),
                onClick = state.onRequestSupport,
                color = TangemTheme.colors.text.accent,
                modifier = Modifier.testTag(ScanFailsDialogTestTags.REQUEST_SUPPORT_BUTTON),
            )

            DialogTextButton(
                text = stringResourceSafe(R.string.common_cancel),
                onClick = state.onDismiss,
                color = TangemTheme.colors.text.warning,
                modifier = Modifier.testTag(ScanFailsDialogTestTags.CANCEL_BUTTON),
            )
        }
    }
}

@Composable
private fun DialogTextButton(text: String, onClick: () -> Unit, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = color,
        style = TangemTheme.typography.button,
        textAlign = TextAlign.Center,
        modifier = modifier.clickable(onClick = onClick),
    )
}

object ScanFailsDialogTestTags {
    const val HOW_TO_SCAN_BUTTON = "scan_fails_how_to_scan_button"
    const val REQUEST_SUPPORT_BUTTON = "scan_fails_request_support_button"
    const val CANCEL_BUTTON = "scan_fails_cancel_button"
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun ScanFailsDialogContentPreview() {
    TangemThemePreview {
        ScanFailsDialogContent(
            state = ScanFailsUM(
                isShown = true,
                onHowToScan = {},
                onRequestSupport = {},
                onDismiss = {},
            ),
        )
    }
}
// endregion Preview