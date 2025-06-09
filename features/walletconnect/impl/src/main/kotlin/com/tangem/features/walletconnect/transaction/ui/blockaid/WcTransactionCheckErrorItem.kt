package com.tangem.features.walletconnect.transaction.ui.blockaid

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcTransactionCheckErrorItem(notificationText: String, modifier: Modifier = Modifier) {
    Notification(
        modifier = modifier
            .fillMaxWidth(),
        config = NotificationConfig(
            title = resourceReference(R.string.wc_malicious_transaction),
            subtitle = TextReference.Str(notificationText),
            iconResId = R.drawable.ic_alert_circle_24,
        ),
        containerColor = TangemColorPalette.Amaranth.copy(alpha = 0.1f),
        titleColor = TangemTheme.colors.text.warning,
        subtitleColor = TangemTheme.colors.text.primary1,
        iconTint = TangemTheme.colors.icon.warning,
    )
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcTransactionCheckErrorItemPreview() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary),
        ) {
            WcTransactionCheckErrorItem("The transaction approves erc20 tokens to a known malicious address")
        }
    }
}