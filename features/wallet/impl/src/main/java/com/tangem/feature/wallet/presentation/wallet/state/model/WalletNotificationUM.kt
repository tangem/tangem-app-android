package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessageButtonUM
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

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

    // region Status
    data object SomeNetworksUnreachable : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "SomeNetworksUnreachableNotification",
            title = resourceReference(id = R.string.warning_some_networks_unreachable_title),
            subtitle = resourceReference(id = R.string.warning_some_networks_unreachable_message),
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.status.attention },
            ),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Status,
    )

    data object UsedOutdatedData : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "UsedOutdatedDataNotification",
            title = resourceReference(R.string.warning_outdated_data_title),
            subtitle = resourceReference(R.string.warning_outdated_data_message),
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_error_sync_default_24,
                tintReference = { TangemTheme.colors2.graphic.status.attention },
            ),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Status,
    )

    data object FailedCardValidation : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "FailedCardValidationNotification",
            title = resourceReference(id = R.string.warning_failed_to_verify_card_title),
            subtitle = resourceReference(id = R.string.warning_failed_to_verify_card_message),
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
            messageEffect = TangemMessageEffect.Warning,
        ),
        type = WalletNotificationType.Status,
    )

    data object DevCard : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "DevCardNotification",
            title = resourceReference(id = R.string.warning_developer_card_title),
            subtitle = resourceReference(id = R.string.warning_developer_card_message),
            messageEffect = TangemMessageEffect.None,
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
        ),
        type = WalletNotificationType.Status,
    )

    data object TestnetCard : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "TestnetCardNotification",
            title = resourceReference(id = R.string.warning_testnet_card_title),
            subtitle = resourceReference(id = R.string.warning_testnet_card_message),
            messageEffect = TangemMessageEffect.None,
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
        ),
        type = WalletNotificationType.Status,
    )

    data object DemoCard : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "DemoCardNotification",
            title = resourceReference(id = R.string.warning_demo_mode_title),
            subtitle = resourceReference(id = R.string.warning_demo_mode_message),
            messageEffect = TangemMessageEffect.None,
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
        ),
        type = WalletNotificationType.Status,
    )
    // endregion

    // region Critical
    data class BackupError(val onClick: () -> Unit) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "BackupErrorNotification",
            title = resourceReference(id = R.string.warning_backup_errors_title),
            subtitle = resourceReference(id = R.string.warning_backup_errors_message),
            messageEffect = TangemMessageEffect.Warning,
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.common_contact_support),
                    type = TangemButtonType.Secondary,
                    onClick = onClick,
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
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.button_start_backup_process),
                    type = TangemButtonType.Secondary,
                    onClick = onClick,
                ),
            ),
        ),
        type = WalletNotificationType.Critical,
    )

    data class LowSignatures(val count: Int) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "LowSignaturesNotification",
            title = resourceReference(id = R.string.warning_low_signatures_title),
            subtitle = resourceReference(
                id = R.string.warning_low_signatures_message,
                formatArgs = wrappedList(count.toString()),
            ),
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Critical,
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
                tintReference = {
                    when (messageEffect) {
                        TangemMessageEffect.Warning -> TangemTheme.colors2.graphic.neutral.primary
                        else -> TangemTheme.colors2.graphic.status.attention
                    }
                },
            ),
            messageEffect = messageEffect,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.hw_activation_need_finish),
                    type = TangemButtonType.Secondary,
                    onClick = onClick,
                ),
            ),
        ),
        type = when (messageEffect) {
            TangemMessageEffect.Warning -> WalletNotificationType.Critical
            else -> WalletNotificationType.Warning
        },
    )

    data class NumberOfSignedHashesIncorrect(val onCloseClick: () -> Unit) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "NumberOfSignedHashesIncorrectNotification",
            title = resourceReference(id = R.string.warning_number_of_signed_hashes_incorrect_title),
            subtitle = resourceReference(id = R.string.warning_number_of_signed_hashes_incorrect_message),
            messageEffect = TangemMessageEffect.Warning,
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.img_knight_shield_32,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
            onCloseClick = onCloseClick,
        ),
        type = WalletNotificationType.Critical,
    )
    // endregion

    // region Warning
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
            messageEffect = TangemMessageEffect.Card,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.common_generate_addresses),
                    type = TangemButtonType.Primary,
                    tangemIconUM = tangemIcon?.let {
                        TangemIconUM.Icon(
                            iconRes = tangemIcon,
                            tintReference = { TangemTheme.colors2.graphic.neutral.primaryInverted },
                        )
                    },
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
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.common_unlock),
                    onClick = onClick,
                    type = TangemButtonType.Primary,
                ),
            ),
            messageEffect = TangemMessageEffect.Card,
            isCentered = true,
        ),
        type = WalletNotificationType.Warning,
    )

    data class TangemPayRefreshNeeded(
        private val onRefreshClick: () -> Unit,
        private val buttonText: TextReference,
        private val shouldShowProgress: Boolean,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "TangemPayRefreshNeeded",
            title = resourceReference(id = R.string.tangempay_sync_needed_title),
            subtitle = resourceReference(id = R.string.tangempay_sync_needed_body),
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = buttonText,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_tangem_24,
                        tintReference = { TangemTheme.colors2.graphic.neutral.primaryInverted },
                    ),
                    onClick = onRefreshClick,
                    type = TangemButtonType.Primary,
                    isLoading = shouldShowProgress,
                ),
            ),
            messageEffect = TangemMessageEffect.Warning,
        ),
        type = WalletNotificationType.Warning,
    )

    data object TangemPayUnreachable : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "TangemPayUnreachable",
            title = resourceReference(id = R.string.tangempay_temporarily_unavailable),
            subtitle = resourceReference(id = R.string.tangempay_service_unreachable_try_later),
            messageEffect = TangemMessageEffect.Warning,
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_alert_circle_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
        ),
        type = WalletNotificationType.Warning,
    )
    // endregion

    // region Promo
    data class AddFunds(val onClick: () -> Unit) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "AddFundsPromoNotification",
            title = resourceReference(id = CoreResR.string.main_add_funds_promo_title),
            subtitle = resourceReference(id = CoreResR.string.main_add_funds_promo_description),
            iconUM = TangemIconUM.Icon(
                iconRes = CoreUiR.drawable.ic_swap_28,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
            messageEffect = TangemMessageEffect.Magic,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = CoreResR.string.common_add_funds),
                    type = TangemButtonType.Secondary,
                    onClick = onClick,
                ),
            ),
        ),
        type = WalletNotificationType.Promo,
    )

    data class NoteMigration(val onClick: () -> Unit) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "NoteMigrationNotification",
            title = resourceReference(R.string.wallet_promo_banner_title),
            subtitle = resourceReference(R.string.wallet_promo_banner_description),
            messageEffect = TangemMessageEffect.Magic,
            isCentered = true,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.wallet_promo_banner_button_title),
                    onClick = onClick,
                    type = TangemButtonType.Primary,
                ),
            ),
        ),
        type = WalletNotificationType.Promo,
    )

    data class TangemPayPromo(
        private val onLaterClick: () -> Unit,
        private val onLearnMoreClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "TangemPayPromo",
            iconUM = TangemIconUM.Image(imageRes = R.drawable.ic_visa_in_banner),
            title = resourceReference(id = R.string.tangempay_onboarding_banner_title),
            subtitle = resourceReference(id = R.string.tangempay_get_banner_description),
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.common_later),
                    onClick = onLaterClick,
                    type = TangemButtonType.Secondary,
                ),
                TangemMessageButtonUM(
                    text = resourceReference(id = R.string.common_learn_more),
                    onClick = onLearnMoreClick,
                    type = TangemButtonType.Primary,
                ),
            ),
            messageEffect = TangemMessageEffect.None,
        ),
        type = WalletNotificationType.Promo,
    )

    data class YieldBoostPromo(
        val onExploreClick: () -> Unit,
        val onLaterClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "YieldBoostPromoNotification",
            title = combinedReference(
                resourceReference(CoreResR.string.yield_apy_boost_banner_title),
                stringReference(" · "),
                resourceReference(CoreResR.string.yield_apy_boost_banner_title_apy_multiplied),
            ),
            subtitle = resourceReference(CoreResR.string.yield_apy_boost_banner_subtitle),
            iconUM = TangemIconUM.Image(imageRes = CoreUiR.drawable.ic_yield_32),
            messageEffect = TangemMessageEffect.Magic,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(CoreResR.string.common_later),
                    type = TangemButtonType.Secondary,
                    onClick = onLaterClick,
                ),
                TangemMessageButtonUM(
                    text = resourceReference(CoreResR.string.yield_apy_boost_banner_button_title),
                    type = TangemButtonType.Primary,
                    onClick = onExploreClick,
                ),
            ),
        ),
        type = WalletNotificationType.Promo,
    )

    // endregion

    // region Survey
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
                    type = TangemButtonType.Secondary,
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
    // endregion

    // region Informational
    data class PushNotifications(
        val onCloseClick: () -> Unit,
        val onEnabledClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "PushNotificationsNotification",
            title = resourceReference(R.string.user_push_notification_banner_title),
            subtitle = resourceReference(R.string.user_push_notification_banner_subtitle),
            iconUM = TangemIconUM.Image(R.drawable.img_push_reminder),
            iconSize = 54.dp,
            messageEffect = TangemMessageEffect.Magic,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.common_later),
                    type = TangemButtonType.Secondary,
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
            title = resourceReference(R.string.warning_clore_migration_title),
            subtitle = resourceReference(R.string.warning_clore_migration_description),
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_attention_default_24,
                tintReference = { TangemTheme.colors2.graphic.status.attention },
            ),
            messageEffect = TangemMessageEffect.None,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.warning_clore_migration_button),
                    onClick = onStartMigrationClick,
                    type = TangemButtonType.Secondary,
                ),
            ),
        ),
        type = WalletNotificationType.Informational,
    )

    data class AssetsDiscoveryCompleted(
        val onCloseClick: () -> Unit,
        val onManageTokensClick: () -> Unit,
    ) : WalletNotificationUM(
        messageUM = TangemMessageUM(
            id = "AssetsDiscoveryCompletedNotification",
            title = resourceReference(R.string.initial_wallet_sync_banner_title),
            subtitle = resourceReference(R.string.initial_wallet_sync_banner_description),
            iconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_check_circle_24,
                tintReference = { TangemTheme.colors2.graphic.status.accent },
            ),
            messageEffect = TangemMessageEffect.None,
            onCloseClick = onCloseClick,
            buttonsUM = persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(R.string.main_manage_tokens),
                    onClick = onManageTokensClick,
                    type = TangemButtonType.Secondary,
                ),
            ),
        ),
        type = WalletNotificationType.Informational,
    )
    // endregion
}