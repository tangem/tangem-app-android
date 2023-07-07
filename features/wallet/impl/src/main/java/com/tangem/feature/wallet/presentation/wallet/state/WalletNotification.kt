package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.notifications.NotificationState
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.feature.wallet.impl.R

/**
 * Wallet notification component state
 *
 * @property state state
 *
[REDACTED_AUTHOR]
 */
sealed class WalletNotification(open val state: NotificationState) {

    /**
     * "Backup the card" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class NeedToBackup(val onClick: () -> Unit) : WalletNotification(
        state = NotificationState.Action(
            title = "Backup your card",
            iconResId = R.drawable.ic_alert_circle_24,
            onClick = onClick,
            tint = TangemColorPalette.Amaranth,
        ),
    )

    /** "Unreachable networks" notification */
    object UnreachableNetworks : WalletNotification(
        state = NotificationState.Simple(
            title = "Some networks are unreachable",
            iconResId = R.drawable.img_attention_20,
            tint = null,
        ),
    )

    /**
     * "Like Tangem App" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class LikeTangemApp(val onClick: () -> Unit) : WalletNotification(
        state = NotificationState.Action(
            title = "Like Tangem App?",
            iconResId = R.drawable.ic_star_24,
            onClick = onClick,
            tint = TangemColorPalette.Tangerine,
        ),
    )

    /**
     * "Scan the card" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class ScanCard(val onClick: () -> Unit) : WalletNotification(
        state = NotificationState.Action(
            title = "Scan your card to continue",
            iconResId = R.drawable.ic_tangem_24,
            onClick = onClick,
        ),
    )
}