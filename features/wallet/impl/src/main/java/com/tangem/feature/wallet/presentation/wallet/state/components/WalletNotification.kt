package com.tangem.feature.wallet.presentation.wallet.state.components

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.feature.wallet.impl.R

/**
 * Wallet notification component state
 *
 * @property config state
 *
* [REDACTED_AUTHOR]
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
            title = resourceReference(id = R.string.warning_developer_card_title),
            subtitle = resourceReference(id = R.string.warning_developer_card_message),
        )

        object FailedCardValidation : Critical(
            title = resourceReference(id = R.string.warning_failed_to_verify_card_title),
            subtitle = resourceReference(id = R.string.warning_failed_to_verify_card_message),
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
            title = resourceReference(id = R.string.warning_no_backup_title),
            subtitle = resourceReference(id = R.string.warning_no_backup_message),
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(id = R.string.button_start_backup_process),
                onClick = onStartBackupClick,
            ),
        )

        object NetworksUnreachable : Warning(
            title = resourceReference(id = R.string.warning_network_unreachable_title),
            subtitle = resourceReference(id = R.string.warning_network_unreachable_message),
        )

        object SomeNetworksUnreachable : Warning(
            title = resourceReference(id = R.string.warning_some_networks_unreachable_title),
            subtitle = resourceReference(id = R.string.warning_some_networks_unreachable_message),
        )

        data class NumberOfSignedHashesIncorrect(val onCloseClick: () -> Unit) : Warning(
            title = resourceReference(id = R.string.warning_number_of_signed_hashes_incorrect_title),
            subtitle = resourceReference(id = R.string.warning_number_of_signed_hashes_incorrect_message),
            onCloseClick = onCloseClick,
        )

        object TestNetCard : Warning(
            title = resourceReference(id = R.string.warning_testnet_card_title),
            subtitle = resourceReference(id = R.string.warning_testnet_card_message),
        )

        data class LowSignatures(val count: Int) : Warning(
            title = resourceReference(id = R.string.warning_low_signatures_title),
            subtitle = resourceReference(
                id = R.string.warning_low_signatures_message,
                formatArgs = wrappedList(count),
            ),
        )
    }

    sealed class Informational(
        title: TextReference,
        subtitle: TextReference,
        buttonsState: NotificationConfig.ButtonsState? = null,
    ) : WalletNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = buttonsState,
        ),
    ) {

        data class MissingAddresses(val missingAddressesCount: Int, val onGenerateClick: () -> Unit) : Informational(
            title = resourceReference(id = R.string.warning_missing_derivation_title),
            subtitle = pluralReference(
                id = R.plurals.warning_missing_derivation_message,
                count = missingAddressesCount,
                formatArgs = wrappedList(missingAddressesCount),
            ),
            buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(id = R.string.common_generate_addresses),
                iconResId = R.drawable.ic_tangem_24,
                onClick = onGenerateClick,
            ),
        )

        data class NoAccount(val network: String, val symbol: String, val amount: String) : Informational(
            title = resourceReference(id = R.string.warning_no_account_title),
            subtitle = resourceReference(
                id = R.string.no_account_generic,
                wrappedList(network, amount, symbol),
            ),
        )

        object DemoCard : Informational(
            title = resourceReference(id = R.string.warning_demo_mode_title),
            subtitle = resourceReference(id = R.string.warning_demo_mode_message),
        )
    }

    data class UnlockWallets(val onClick: () -> Unit) : WalletNotification(
        config = NotificationConfig(
            title = resourceReference(id = R.string.common_access_denied),
            subtitle = resourceReference(
                id = R.string.warning_access_denied_message,
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
                primaryText = resourceReference(id = R.string.warning_button_like_it),
                onPrimaryClick = onLikeClick,
                secondaryText = resourceReference(id = R.string.warning_button_could_be_better),
                onSecondaryClick = onDislikeClick,
            ),
            onCloseClick = onCloseClick,
        ),
    )
}
