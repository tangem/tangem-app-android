package com.tangem.feature.wallet.presentation.wallet.state.components

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.feature.wallet.impl.R

/**
 * Wallet notification component state
 *
 * @property config state
 *
[REDACTED_AUTHOR]
 */
// TODO: Finalize notification strings [REDACTED_JIRA]
@Immutable
sealed class WalletNotification(open val config: NotificationConfig) {

    /** Clickable notification */
    sealed interface Clickable {

        /** Lambda be invoked when notification is clicked */
        val onClick: () -> Unit
    }

    /** "Development card" notification */
    object DevCard : WalletNotification(
        config = NotificationConfig(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(id = R.string.alert_developer_card),
            iconResId = R.drawable.ic_alert_circle_24,
        ),
    )

    /** "Test card" notification */
    object TestCard : WalletNotification(
        config = NotificationConfig(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(id = R.string.warning_testnet_card_message),
            iconResId = R.drawable.ic_alert_circle_24,
        ),
    )

    /** "Demo card" notification */
    object DemoCard : WalletNotification(
        config = NotificationConfig(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(id = R.string.alert_demo_message),
            iconResId = R.drawable.ic_alert_circle_24,
        ),
    )

    /** "Card verification failed" notification */
    object CardVerificationFailed : WalletNotification(
        config = NotificationConfig(
            title = TextReference.Res(id = R.string.warning_failed_to_verify_card_title),
            subtitle = TextReference.Res(id = R.string.warning_failed_to_verify_card_message),
            iconResId = R.drawable.ic_alert_circle_24,
        ),
    )

    /**
     * "Remaining signatures left" notification
     *
     * @param count number of remaining signatures
     */
    class RemainingSignaturesLeft(count: Int) : WalletNotification(
        config = NotificationConfig(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(
                id = R.string.warning_low_signatures_format,
                formatArgs = WrappedList(data = listOf(count)),
            ),
            iconResId = R.drawable.ic_alert_circle_24,
        ),
    )

    /**
     * "Already topped up and signed hashes" warning notification
     *
     * @property onClick lambda be invoked when notification's close button is clicked
     */
    data class WarningAlreadySignedHashes(override val onClick: () -> Unit) : Clickable, WalletNotification(
        config = NotificationConfig(
            title = TextReference.Res(id = R.string.common_warning),
            subtitle = TextReference.Res(id = R.string.alert_card_signed_transactions),
            iconResId = R.drawable.img_attention_20,
            onCloseClick = onClick,
        ),
    )

    /**
     * "Already signed hashes" critical warning notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class CriticalWarningAlreadySignedHashes(override val onClick: () -> Unit) : Clickable, WalletNotification(
        config = NotificationConfig(
            title = TextReference.Res(
                id = R.string.warning_important_security_info,
                formatArgs = WrappedList(listOf("\u26A0")),
            ),
            subtitle = TextReference.Res(id = R.string.warning_signed_tx_previously),
            iconResId = R.drawable.img_attention_20,
        ),
    )

    /**
     * "Backup the card" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class BackupCard(override val onClick: () -> Unit) : Clickable, WalletNotification(
        config = NotificationConfig(
            title = TextReference.Str(value = "Backup your card"),
            subtitle = TextReference.Str(value = ""),
            iconResId = R.drawable.img_attention_20,
        ),
    )

    /** "Unreachable networks" notification */
    object UnreachableNetworks : WalletNotification(
        config = NotificationConfig(
            title = TextReference.Str(value = "Some networks are unreachable"),
            subtitle = TextReference.Str(value = ""),
            iconResId = R.drawable.img_attention_20,
        ),
    )

    /**
     * "Like Tangem App" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class LikeTangemApp(override val onClick: () -> Unit) : Clickable, WalletNotification(
        config = NotificationConfig(
            title = TextReference.Str(value = "Like Tangem App?"),
            subtitle = TextReference.Str(value = ""),
            iconResId = R.drawable.ic_star_24,
        ),
    )

    /**
     * "Scan the card" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class ScanCard(override val onClick: () -> Unit) : Clickable, WalletNotification(
        config = NotificationConfig(
            title = TextReference.Str(value = "Scan your card to continue"),
            subtitle = TextReference.Str(value = ""),
            iconResId = R.drawable.ic_tangem_24,
        ),
    )

    /**
     * "Unlock wallets" notification
     *
     * @property onClick lambda be invoked when notification is clicked
     */
    data class UnlockWallets(override val onClick: () -> Unit) : Clickable, WalletNotification(
        config = NotificationConfig(
            title = TextReference.Str(value = "Unlock needed"),
            subtitle = TextReference.Str(value = ""),
            iconResId = R.drawable.ic_locked_24,
        ),
    )
}