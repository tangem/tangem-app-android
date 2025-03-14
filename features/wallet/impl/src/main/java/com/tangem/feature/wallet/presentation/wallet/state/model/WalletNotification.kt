package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.feature.wallet.impl.R
import org.joda.time.DateTime

/**
 * Wallet notification component state
 *
 * @property config state
 *
[REDACTED_AUTHOR]
 */
@Immutable
sealed class WalletNotification(val config: NotificationConfig) {

    sealed class Critical(
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

        data object DevCard : Critical(
            title = resourceReference(id = R.string.warning_developer_card_title),
            subtitle = resourceReference(id = R.string.warning_developer_card_message),
        )

        data object FailedCardValidation : Critical(
            title = resourceReference(id = R.string.warning_failed_to_verify_card_title),
            subtitle = resourceReference(id = R.string.warning_failed_to_verify_card_message),
        )

        data class BackupError(val onSupportClick: () -> Unit) : Critical(
            title = resourceReference(R.string.warning_backup_errors_title),
            subtitle = resourceReference(R.string.warning_backup_errors_message),
            buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(id = R.string.details_row_title_contact_to_support),
                onClick = onSupportClick,
            ),
        )

        data class SeedPhraseNotification(
            val onDeclineClick: () -> Unit,
            val onConfirmClick: () -> Unit,
        ) : Critical(
            title = resourceReference(R.string.warning_seedphrase_issue_title),
            subtitle = resourceReference(R.string.warning_seedphrase_issue_message),
            buttonsState = NotificationConfig.ButtonsState.SecondaryPairButtonsConfig(
                leftText = resourceReference(R.string.common_no),
                onLeftClick = onDeclineClick,
                rightText = resourceReference(R.string.common_yes),
                onRightClick = onConfirmClick,
            ),
        )

        data class SeedPhraseSecondNotification(
            val onDeclineClick: () -> Unit,
            val onConfirmClick: () -> Unit,
        ) : Critical(
            title = resourceReference(R.string.warning_seedphrase_action_required_title),
            subtitle = resourceReference(R.string.warning_seedphrase_contacted_support),
            buttonsState = NotificationConfig.ButtonsState.SecondaryPairButtonsConfig(
                leftText = resourceReference(R.string.seed_warning_no),
                onLeftClick = onDeclineClick,
                rightText = resourceReference(R.string.seed_warning_yes),
                onRightClick = onConfirmClick,
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
            title = resourceReference(id = R.string.warning_no_backup_title),
            subtitle = resourceReference(id = R.string.warning_no_backup_message),
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(id = R.string.button_start_backup_process),
                onClick = onStartBackupClick,
            ),
        )

        data object NetworksUnreachable : Warning(
            title = resourceReference(id = R.string.warning_network_unreachable_title),
            subtitle = resourceReference(id = R.string.warning_network_unreachable_message),
        )

        data object SomeNetworksUnreachable : Warning(
            title = resourceReference(id = R.string.warning_some_networks_unreachable_title),
            subtitle = resourceReference(id = R.string.warning_some_networks_unreachable_message),
        )

        data class NumberOfSignedHashesIncorrect(val onCloseClick: () -> Unit) : Warning(
            title = resourceReference(id = R.string.warning_number_of_signed_hashes_incorrect_title),
            subtitle = resourceReference(id = R.string.warning_number_of_signed_hashes_incorrect_message),
            onCloseClick = onCloseClick,
        )

        data object TestNetCard : Warning(
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

        data object DemoCard : Informational(
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

    data class UnlockVisaAccess(val onUnlockClick: () -> Unit) : WalletNotification(
        config = NotificationConfig(
            title = resourceReference(id = R.string.visa_unlock_notification_title),
            subtitle = resourceReference(id = R.string.visa_unlock_notification_subtitle),
            iconResId = R.drawable.ic_locked_24,
            buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(id = R.string.visa_unlock_notification_button),
                iconResId = R.drawable.ic_tangem_24,
                onClick = onUnlockClick,
            ),
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

    data class SwapPromo(
        val startDateTime: DateTime,
        val endDateTime: DateTime,
        val onCloseClick: () -> Unit,
    ) : WalletNotification(
        config = NotificationConfig(
            title = resourceReference(id = R.string.swap_promo_title),
            subtitle = resourceReference(id = R.string.swap_promo_text),
            iconResId = R.drawable.img_okx_dex_logo,
            onCloseClick = onCloseClick,
        ),
    )

    data class NoteMigration(val onClick: () -> Unit) : WalletNotification(
        config = NotificationConfig(
            title = resourceReference(R.string.wallet_promo_banner_title),
            subtitle = resourceReference(R.string.wallet_promo_banner_description),
            iconResId = R.drawable.banner_note_migration,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.wallet_promo_banner_button_title),
                onClick = onClick,
            ),
        ),
    )

    data object UsedOutdatedData : WalletNotification(
        config = NotificationConfig(
            subtitle = resourceReference(R.string.warning_some_token_balances_not_updated),
            iconResId = R.drawable.ic_error_sync_24,
        ),
    )
}