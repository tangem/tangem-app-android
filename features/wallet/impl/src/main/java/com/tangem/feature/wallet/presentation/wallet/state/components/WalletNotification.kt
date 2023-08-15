package com.tangem.feature.wallet.presentation.wallet.state.components

import com.tangem.core.ui.components.notifications.NotificationState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.feature.wallet.impl.R

/**
 * Wallet notification component state
 *
 * @property state state
 *
[REDACTED_AUTHOR]
 */
// TODO: Finalize notification strings [REDACTED_JIRA]
sealed class WalletNotification(open val state: NotificationState) {

    /** Clickable notification */
    sealed interface Clickable {

        /** Lambda be invoked when notification is clicked */
        val onClick: () -> Unit
    }

    /** "Development card" notification */
    object DevCard : WalletNotification(
        state = NotificationState.Simple(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(id = R.string.alert_developer_card),
            iconResId = R.drawable.ic_alert_circle_24,
            tint = TangemColorPalette.Amaranth,
        ),
    )

    /** "Test card" notification */
    object TestCard : WalletNotification(
        state = NotificationState.Simple(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(id = R.string.warning_testnet_card_message),
            iconResId = R.drawable.ic_alert_circle_24,
            tint = TangemColorPalette.Amaranth,
        ),
    )

    /** "Demo card" notification */
    object DemoCard : WalletNotification(
        state = NotificationState.Simple(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(id = R.string.alert_demo_message),
            iconResId = R.drawable.ic_alert_circle_24,
            tint = TangemColorPalette.Amaranth,
        ),
    )

    /** "Card verification failed" notification */
    object CardVerificationFailed : WalletNotification(
        state = NotificationState.Simple(
            title = TextReference.Res(id = R.string.warning_failed_to_verify_card_title),
            subtitle = TextReference.Res(id = R.string.warning_failed_to_verify_card_message),
            iconResId = R.drawable.ic_alert_circle_24,
            tint = TangemColorPalette.Amaranth,
        ),
    )

    /**
     * "Remaining signatures left" notification
     *
     * @param count number of remaining signatures
     */
    class RemainingSignaturesLeft(count: Int) : WalletNotification(
        state = NotificationState.Simple(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(
                id = R.string.warning_low_signatures_format,
                formatArgs = WrappedList(data = listOf(count)),
            ),
            iconResId = R.drawable.ic_alert_circle_24,
            tint = TangemColorPalette.Amaranth,
        ),
    )

    /**
     * "Already topped up and signed hashes" warning notification
     *
     * @property onClick lambda be invoked when notification's close button is clicked
     */
    data class WarningAlreadySignedHashes(override val onClick: () -> Unit) : Clickable, WalletNotification(
        state = NotificationState.Closable(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(id = R.string.alert_card_signed_transactions),
            iconResId = R.drawable.img_attention_20,
            tint = null,
            onCloseClick = onClick,
        ),
    )

    /**
     * "Already signed hashes" critical warning notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class CriticalWarningAlreadySignedHashes(override val onClick: () -> Unit) : Clickable, WalletNotification(
        state = NotificationState.Clickable(
            title = TextReference.Res(
                id = R.string.warning_important_security_info,
                formatArgs = WrappedList(listOf("\u26A0")),
            ),
            subtitle = TextReference.Res(id = R.string.warning_signed_tx_previously),
            iconResId = R.drawable.img_attention_20,
            onClick = onClick,
            tint = null,
        ),
    )

    /**
     * "Backup the card" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class BackupCard(override val onClick: () -> Unit) : Clickable, WalletNotification(
        state = NotificationState.Clickable(
            title = TextReference.Str(value = "Backup your card"),
            iconResId = R.drawable.img_attention_20,
            onClick = onClick,
            tint = null,
        ),
    )

    /** "Unreachable networks" notification */
    object UnreachableNetworks : WalletNotification(
        state = NotificationState.Simple(
            title = TextReference.Str(value = "Some networks are unreachable"),
            iconResId = R.drawable.img_attention_20,
            tint = null,
        ),
    )

    /**
     * "Like Tangem App" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class LikeTangemApp(override val onClick: () -> Unit) : Clickable, WalletNotification(
        state = NotificationState.Clickable(
            title = TextReference.Str(value = "Like Tangem App?"),
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
    data class ScanCard(override val onClick: () -> Unit) : Clickable, WalletNotification(
        state = NotificationState.Clickable(
            title = TextReference.Str(value = "Scan your card to continue"),
            iconResId = R.drawable.ic_tangem_24,
            onClick = onClick,
        ),
    )

    /**
     * "Unlock wallets" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class UnlockWallets(override val onClick: () -> Unit) : Clickable, WalletNotification(
        state = NotificationState.Clickable(
            title = TextReference.Str(value = "Unlock needed"),
            iconResId = R.drawable.ic_locked_24,
            onClick = onClick,
        ),
    )
}