package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.annotation.DrawableRes
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessageButtonUM
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import kotlinx.collections.immutable.persistentListOf

/**
 * Wallet notification types
 */
internal enum class WalletNotificationType {
    Status,
    Critical,
    Warning,
    Promo,
    Survey,
    Informational,
}

/**
 * Wallet notification UI model
 *
 * @property messageUM - message to show in notification
 * @property type - type of notification, affects design and priority
 */
internal sealed class WalletNotificationUM(val messageUM: TangemMessageUM, val type: WalletNotificationType) {

    data object DevCard : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "DevCardNotification",
            title = resourceReference(id = R.string.warning_developer_card_title),
            subtitle = resourceReference(id = R.string.warning_developer_card_message),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Warning,
    )

    data object FailedCardValidation : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "FailedCardValidationNotification",
            title = resourceReference(id = R.string.warning_failed_to_verify_card_title),
            subtitle = resourceReference(id = R.string.warning_failed_to_verify_card_message),
            messageEffect = TangemMessageEffect.Warning,
        ),
        type = WalletNotificationType.Status,
    )

    data class BackupError(val onClick: () -> Unit) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "BackupErrorNotification",
            title = resourceReference(id = R.string.warning_backup_errors_title),
            subtitle = resourceReference(id = R.string.warning_backup_errors_message),
            messageEffect = TangemMessageEffect.Warning,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.common_contact_support),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onClick,
                ),
            ),
        ),
        type = WalletNotificationType.Warning,
    )

    data class SeedPhraseNotification(
        val onDeclineClick: () -> Unit,
        val onConfirmClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "SeedPhraseIssueNotification",
            title = resourceReference(id = R.string.warning_seedphrase_issue_title),
            subtitle = resourceReference(id = R.string.warning_seedphrase_issue_message),
            messageEffect = TangemMessageEffect.Warning,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.common_no),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onDeclineClick,
                ),
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.common_yes),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onConfirmClick,
                ),
            ),
        ),
        type = WalletNotificationType.Critical,
    )

    data class SeedPhraseSecondNotification(
        val onDeclineClick: () -> Unit,
        val onConfirmClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "SeedPhraseSecondIssueNotification",
            title = resourceReference(id = R.string.warning_seedphrase_action_required_title),
            subtitle = resourceReference(id = R.string.warning_seedphrase_contacted_support),
            messageEffect = TangemMessageEffect.Warning,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.seed_warning_no),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onDeclineClick,
                ),
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.seed_warning_yes),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onConfirmClick,
                ),
            ),
        ),
        type = WalletNotificationType.Critical,
    )

    data class MissingBackup(val onClick: () -> Unit) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "MissingBackupNotification",
            title = resourceReference(id = R.string.warning_no_backup_title),
            subtitle = resourceReference(id = R.string.warning_no_backup_message),
            messageEffect = TangemMessageEffect.Warning,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.button_start_backup_process),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onClick,
                ),
            ),
        ),
        type = WalletNotificationType.Critical,
    )

    data object SomeNetworksUnreachable : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "SomeNetworksUnreachableNotification",
            title = resourceReference(id = R.string.warning_some_networks_unreachable_title),
            subtitle = resourceReference(id = R.string.warning_some_networks_unreachable_message),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Status,
    )

    data class NumberOfSignedHashesIncorrect(val onCloseClick: () -> Unit) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "NumberOfSignedHashesIncorrectNotification",
            title = resourceReference(id = R.string.warning_number_of_signed_hashes_incorrect_title),
            subtitle = resourceReference(id = R.string.warning_number_of_signed_hashes_incorrect_message),
            messageEffect = TangemMessageEffect.Warning,
            onCloseClick = onCloseClick,
        ),
        type = WalletNotificationType.Warning,
    )

    data object TestnetCard : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "TestnetCardNotification",
            title = resourceReference(id = R.string.warning_testnet_card_title),
            subtitle = resourceReference(id = R.string.warning_testnet_card_message),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Warning,
    )

    data class LowSignatures(val count: Int) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "LowSignaturesNotification",
            title = resourceReference(id = R.string.warning_low_signatures_title),
            subtitle = resourceReference(
                id = R.string.warning_low_signatures_message,
                formatArgs = wrappedList(count.toString()),
            ),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Critical,
    )

    data class MissingAddresses(
        @DrawableRes val tangemIcon: Int?,
        val missingAddressesCount: Int,
        val onGenerateClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "MissingAddressesNotification",
            title = resourceReference(id = R.string.warning_missing_derivation_title),
            subtitle = pluralReference(
                id = R.plurals.warning_missing_derivation_message,
                count = missingAddressesCount,
                formatArgs = wrappedList(missingAddressesCount),
            ),
            isCentered = true,
            iconUM = tangemIcon?.let { TangemIconUM.Icon(it) },
            messageEffect = TangemMessageEffect.Magic,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.common_generate_addresses),
                    type = TangemButtonType.PrimaryInverse,
                    iconRes = tangemIcon,
                    onClick = onGenerateClick,
                ),
            ),
        ),
        type = WalletNotificationType.Warning,
    )

    data class NoAccount(val network: String, val symbol: String, val amount: String) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "NoAccountNotification",
            title = resourceReference(id = R.string.warning_no_account_title),
            subtitle = resourceReference(
                id = R.string.no_account_generic,
                wrappedList(network, amount, symbol),
            ),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Warning,
    )

    data object DemoCard : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "DemoCardNotification",
            title = resourceReference(id = R.string.warning_demo_mode_title),
            subtitle = resourceReference(id = R.string.warning_demo_mode_title),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Warning,
    )

    data class NoteMigration(val onClick: () -> Unit) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "NoteMigrationNotification",
            title = resourceReference(R.string.wallet_promo_banner_title),
            subtitle = resourceReference(R.string.wallet_promo_banner_description),
            messageEffect = TangemMessageEffect.None,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.wallet_promo_banner_button_title),
                    onClick = onClick,
                    type = TangemButtonType.PrimaryInverse,
                ),
            ),
        ),
        type = WalletNotificationType.Promo,
    )

    data class UnlockWallets(val onClick: () -> Unit) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "UnlockWalletsNotification",
            title = resourceReference(id = R.string.common_access_denied),
            subtitle = resourceReference(
                id = R.string.warning_access_denied_message,
                formatArgs = wrappedList(
                    resourceReference(R.string.common_biometrics),
                ),
            ),
            onClick = onClick,
            messageEffect = TangemMessageEffect.Magic,
            isCentered = true,
        ),
        type = WalletNotificationType.Warning,
    )

    data class RateApp(
        val onLikeClick: () -> Unit,
        val onDislikeClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "RateAppNotification",
            title = resourceReference(id = R.string.warning_rate_app_title),
            subtitle = resourceReference(id = R.string.warning_rate_app_message),
            isCentered = true,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.warning_button_could_be_better),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onDislikeClick,
                ),
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.warning_button_like_it),
                    type = TangemButtonType.Primary,
                    onClick = onLikeClick,
                ),
            ),
            messageEffect = TangemMessageEffect.None,
            onCloseClick = onCloseClick,
        ),
        type = WalletNotificationType.Survey,
    )

    data object UsedOutdatedData : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "UsedOutdatedDataNotification",
            title = stringReference("Missing some token balances"), // todo redesign main lokalise
            subtitle = stringReference("Will be updated as soon as possible"),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Status,
    )

    data class FinishWalletActivation(
        val messageEffect: TangemMessageEffect,
        val isBackupExists: Boolean,
        val onClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "FinishWalletActivationNotification",
            title = resourceReference(R.string.hw_activation_need_title),
            subtitle = if (isBackupExists) {
                resourceReference(R.string.hw_activation_need_warning_description)
            } else {
                resourceReference(R.string.hw_activation_need_description)
            },
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.img_knight_shield_32,
                tintReference = { TangemTheme.colors2.graphic.status.attention },
            ),
            messageEffect = messageEffect,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.hw_activation_need_finish),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onClick,
                ),
            ),
        ),
        type = when (messageEffect) {
            TangemMessageEffect.Card -> WalletNotificationType.Critical
            else -> WalletNotificationType.Warning
        },
    )

    data class PushNotifications(
        val onCloseClick: () -> Unit,
        val onEnabledClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "PushNotificationsNotification",
            title = resourceReference(R.string.user_push_notification_banner_title),
            subtitle = resourceReference(R.string.user_push_notification_banner_subtitle),
            onCloseClick = onCloseClick,
            messageEffect = TangemMessageEffect.Card,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.common_later),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onCloseClick,
                ),
                TangemMessageButtonUM(
                    text = resourceReference(R.string.common_enable),
                    type = TangemButtonType.Primary,
                    onClick = onEnabledClick,
                ),
            ),
        ),
        type = WalletNotificationType.Informational,
    )

    data class CloreMigration(
        val onStartMigrationClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "CloreMigrationNotification",
            title = resourceReference(com.tangem.core.res.R.string.warning_clore_migration_title),
            subtitle = resourceReference(com.tangem.core.res.R.string.warning_clore_migration_description),
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.status.attention },
            ),
            messageEffect = TangemMessageEffect.None,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(com.tangem.core.res.R.string.warning_clore_migration_button),
                    onClick = onStartMigrationClick,
                    type = TangemButtonType.PrimaryInverse,
                ),
            ),
        ),
        type = WalletNotificationType.Informational,
    )

    data class OnePlusOnePromo(
        val onCloseClick: () -> Unit,
        val onClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "OnePlusOnePromoNotification",
            title = resourceReference(R.string.notification_one_plus_one_title),
            subtitle = resourceReference(R.string.notification_one_plus_one_text),
            messageEffect = TangemMessageEffect.Magic,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.notification_one_plus_one_button),
                    type = TangemButtonType.PrimaryInverse,
                    onClick = onCloseClick,
                ),
                TangemMessageButtonUM(
                    text = resourceReference(R.string.notification_one_plus_one_button),
                    type = TangemButtonType.Primary,
                    onClick = onClick,
                ),
            ),
        ),
        type = WalletNotificationType.Promo,
    )

    data class YieldPromo(
        val onCloseClick: () -> Unit,
        val onTermsAndConditionsClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "YieldPromoNotification",
            title = resourceReference(R.string.notification_yield_promo_title),
            subtitle = resourceReference(R.string.notification_yield_promo_text),
            onCloseClick = onCloseClick,
            messageEffect = TangemMessageEffect.Magic,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.notification_yield_promo_button),
                    type = TangemButtonType.Primary,
                    onClick = onTermsAndConditionsClick,
                ),
            ),
        ),
        type = WalletNotificationType.Promo,
    )
}