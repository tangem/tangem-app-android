package com.tangem.feature.wallet.presentation.wallet.state.components

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.*
import com.tangem.feature.wallet.impl.R

/**
 * Wallet notification component state
 *
 * @property config state
 *
[REDACTED_AUTHOR]
 */
@Immutable
sealed class WalletNotification(val config: NotificationConfig) {

    sealed class Critical(title: TextReference, subtitle: TextReference) : WalletNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.ic_alert_circle_24,
        ),
    ) {

        object DevCard : Critical(
            title = resourceReference(id = R.string.common_warning),
            subtitle = resourceReference(id = R.string.alert_developer_card),
        )

        object DemoCard : Critical(
            title = resourceReference(id = R.string.common_warning),
            subtitle = resourceReference(id = R.string.alert_demo_message),
        )

        object TestNetCard : Critical(
            title = resourceReference(id = R.string.common_warning),
            subtitle = resourceReference(id = R.string.warning_testnet_card_message),
        )

        object FailedCardValidation : Critical(
            title = resourceReference(id = R.string.warning_failed_to_verify_card_title),
            subtitle = resourceReference(id = R.string.warning_failed_to_verify_card_message),
        )

        data class LowSignatures(val count: Int) : Critical(
            title = resourceReference(id = R.string.common_warning),
            subtitle = resourceReference(
                id = R.string.warning_low_signatures_format,
                formatArgs = wrappedList(count),
            ),
        )
    }

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onClick: (() -> Unit)? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : WalletNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.img_attention_20,
            buttonsState = buttonsState,
            onClick = onClick,
            onCloseClick = onCloseClick,
        ),
    ) {

        data class MissingBackup(val onStartBackupClick: () -> Unit) : Warning(
            title = resourceReference(id = R.string.main_no_backup_warning_title),
            subtitle = resourceReference(id = R.string.main_no_backup_warning_subtitle),
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(id = R.string.button_start_backup_process),
                onClick = onStartBackupClick,
            ),
        )

        object NetworksUnreachable : Warning(
            title = resourceReference(id = R.string.wallet_balance_blockchain_unreachable),
            subtitle = resourceReference(id = R.string.warning_subtitle_network_unreachable),
        )

        object SomeNetworksUnreachable : Warning(
            title = resourceReference(id = R.string.warning_title_some_networks_unreachable),
            subtitle = resourceReference(id = R.string.warning_subtitle_some_networks_unreachable),
        )

        data class TopUpNote(val errorMessage: String) : Warning(
            title = resourceReference(id = R.string.warning_title_note_top_up),
            subtitle = stringReference(value = errorMessage),
        )

        data class NumberOfSignedHashesIncorrect(val onCloseClick: () -> Unit) : Warning(
            title = resourceReference(id = R.string.common_warning),
            subtitle = resourceReference(id = R.string.alert_card_signed_transactions),
            onCloseClick = onCloseClick,
        )
    }

    data class MissingAddresses(val missingAddressesCount: Int, val onGenerateClick: () -> Unit) : WalletNotification(
        config = NotificationConfig(
            title = resourceReference(id = R.string.main_warning_missing_derivation_title),
            subtitle = pluralReference(
                id = R.plurals.main_warning_missing_derivation_description,
                count = missingAddressesCount,
                formatArgs = wrappedList(missingAddressesCount),
            ),
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(id = R.string.common_generate_addresses),
                iconResId = R.drawable.ic_tangem_24,
                onClick = onGenerateClick,
            ),
        ),
    )

    data class UnlockWallets(val onClick: () -> Unit) : WalletNotification(
        config = NotificationConfig(
            title = resourceReference(id = R.string.common_unlock_needed),
            subtitle = resourceReference(
                id = R.string.unlock_wallet_description_short,
                formatArgs = wrappedList(
                    resourceReference(R.string.common_biometrics),
                ),
            ),
            iconResId = R.drawable.ic_locked_24,
            onClick = onClick,
        ),
    )

    data class RateApp(
        val onLikeClick: () -> Unit,
        val onDislikeClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : WalletNotification(
        config = NotificationConfig(
            title = resourceReference(id = R.string.warning_rate_app_title),
            subtitle = resourceReference(id = R.string.warning_rate_app_message),
            iconResId = R.drawable.ic_star_24,
            buttonsState = NotificationConfig.ButtonsState.PairButtonsConfig(
                primaryText = resourceReference(id = R.string.warning_button_love_it),
                onPrimaryClick = onLikeClick,
                secondaryText = resourceReference(id = R.string.warning_button_can_be_better),
                onSecondaryClick = onDislikeClick,
            ),
            onCloseClick = onCloseClick,
        ),
    )
}