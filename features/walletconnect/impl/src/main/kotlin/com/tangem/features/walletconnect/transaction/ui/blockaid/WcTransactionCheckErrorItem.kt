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
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.blockaid.BlockAidNotificationUM

@Composable
internal fun WcTransactionCheckErrorItem(notification: BlockAidNotificationUM, modifier: Modifier = Modifier) {
    Notification(
        modifier = modifier
            .fillMaxWidth(),
        config = NotificationConfig(
            title = notification.title,
            subtitle = TextReference.Str(notification.text?.resolveReference() ?: ""),
            iconResId = when (notification.type) {
                BlockAidNotificationUM.Type.ERROR -> R.drawable.ic_alert_circle_24
                BlockAidNotificationUM.Type.WARNING -> R.drawable.ic_alert_triangle_20
            },
        ),
        containerColor = when (notification.type) {
            BlockAidNotificationUM.Type.ERROR -> TangemColorPalette.Amaranth.copy(alpha = 0.1f)
            BlockAidNotificationUM.Type.WARNING -> TangemColorPalette.Dark1.copy(alpha = 0.1f)
        },
        titleColor = when (notification.type) {
            BlockAidNotificationUM.Type.ERROR -> TangemTheme.colors.text.warning
            BlockAidNotificationUM.Type.WARNING -> TangemTheme.colors.text.primary1
        },
        subtitleColor = when (notification.type) {
            BlockAidNotificationUM.Type.ERROR -> TangemTheme.colors.text.primary1
            BlockAidNotificationUM.Type.WARNING -> TangemTheme.colors.text.tertiary
        },
        iconTint = when (notification.type) {
            BlockAidNotificationUM.Type.ERROR -> TangemTheme.colors.icon.warning
            BlockAidNotificationUM.Type.WARNING -> TangemTheme.colors.icon.attention
        },
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
            WcTransactionCheckErrorItem(
                BlockAidNotificationUM(
                    type = BlockAidNotificationUM.Type.ERROR,
                    title = TextReference.Res(R.string.wc_malicious_transaction),
                    text = TextReference.Str("The transaction approves erc20 tokens to a known malicious address"),
                ),
            )
        }
    }
}