package com.tangem.feature.walletsettings.utils

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.hot.sdk.model.HotWalletId
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@ModelScoped
internal class ItemsBuilder @Inject constructor() {

    @Suppress("LongParameterList")
    fun buildItems(
        userWallet: UserWallet,
        cardItem: WalletSettingsItemUM.CardBlock,
        accountsUM: List<WalletSettingsAccountsUM>,
        isLinkMoreCardsAvailable: Boolean,
        isReferralAvailable: Boolean,
        isManageTokensAvailable: Boolean,
        isNFTFeatureEnabled: Boolean,
        isNFTEnabled: Boolean,
        onCheckedNFTChange: (Boolean) -> Unit,
        isNotificationsEnabled: Boolean,
        isNotificationsPermissionGranted: Boolean,
        onCheckedNotificationsChanged: (Boolean) -> Unit,
        onNotificationsDescriptionClick: () -> Unit,
        forgetWallet: () -> Unit,
        onLinkMoreCardsClick: () -> Unit,
        onReferralClick: () -> Unit,
        onManageTokensClick: () -> Unit,
        onAccessCodeClick: () -> Unit,
        onBackupClick: () -> Unit,
        onCardSettingsClick: () -> Unit,
    ): PersistentList<WalletSettingsItemUM> = persistentListOf<WalletSettingsItemUM>()
        .add(cardItem)
        .addAll(buildAccessCodeItem(userWallet, onAccessCodeClick))
        .addAll(accountsUM)
        .add(
            buildCardItem(
                userWallet = userWallet,
                isLinkMoreCardsAvailable = isLinkMoreCardsAvailable,
                isReferralAvailable = isReferralAvailable,
                isManageTokensAvailable = isManageTokensAvailable,
                onLinkMoreCardsClick = onLinkMoreCardsClick,
                onReferralClick = onReferralClick,
                onManageTokensClick = onManageTokensClick,
                onBackupClick = onBackupClick,
                onCardSettingsClick = onCardSettingsClick,
            ),
        )
        .addAll(
            buildNotificationItems(
                isNotificationsPermissionGranted = isNotificationsPermissionGranted,
                isNotificationsEnabled = isNotificationsEnabled,
                onCheckedNotificationsChanged = onCheckedNotificationsChanged,
                onNotificationsDescriptionClick = onNotificationsDescriptionClick,
            ),
        )
        .addAll(
            buildNFTItems(
                isNFTFeatureEnabled = isNFTFeatureEnabled,
                isNFTEnabled = isNFTEnabled,
                onCheckedNFTChange = onCheckedNFTChange,
            ),
        )
        .add(buildForgetItem(forgetWallet))

    private fun buildNFTItems(
        isNFTFeatureEnabled: Boolean,
        isNFTEnabled: Boolean,
        onCheckedNFTChange: (Boolean) -> Unit,
    ): List<WalletSettingsItemUM> {
        return if (isNFTFeatureEnabled) {
            listOf(buildNFTItem(isNFTEnabled, onCheckedNFTChange))
        } else {
            emptyList()
        }
    }

    private fun buildNotificationItems(
        isNotificationsPermissionGranted: Boolean,
        isNotificationsEnabled: Boolean,
        onCheckedNotificationsChanged: (Boolean) -> Unit,
        onNotificationsDescriptionClick: () -> Unit,
    ): List<WalletSettingsItemUM> {
        return buildList {
            if (!isNotificationsPermissionGranted) {
                add(buildNotificationsPermissionItem())
            }
            add(buildNotificationsSwitchItem(isNotificationsEnabled, onCheckedNotificationsChanged))
            add(buildNotificationsDescriptionItem(onNotificationsDescriptionClick))
        }
    }

    private fun buildNFTItem(isNFTEnabled: Boolean, onCheckedNFTChange: (Boolean) -> Unit) =
        WalletSettingsItemUM.WithSwitch(
            id = "nft",
            title = resourceReference(id = R.string.details_nft_title),
            isChecked = isNFTEnabled,
            onCheckedChange = onCheckedNFTChange,
        )

    private fun buildNotificationsPermissionItem() = WalletSettingsItemUM.NotificationPermission(
        id = "notifications_permission",
        title = resourceReference(id = R.string.transaction_notifications_warning_title),
        description = resourceReference(id = R.string.transaction_notifications_warning_description),
    )

    private fun buildNotificationsSwitchItem(isNFTEnabled: Boolean, onCheckedNFTChange: (Boolean) -> Unit) =
        WalletSettingsItemUM.WithSwitch(
            id = "notifications",
            title = resourceReference(id = R.string.wallet_settings_push_notifications_title),
            isChecked = isNFTEnabled,
            onCheckedChange = onCheckedNFTChange,
        )

    private fun buildNotificationsDescriptionItem(onDescriptionClick: () -> Unit) =
        WalletSettingsItemUM.DescriptionWithMore(
            id = "notifications_description",
            text = resourceReference(id = R.string.wallet_settings_push_notifications_description),
            more = resourceReference(id = R.string.push_notifications_more_info),
            onClick = onDescriptionClick,
        )

    @Suppress("LongParameterList")
    private fun buildCardItem(
        userWallet: UserWallet,
        isLinkMoreCardsAvailable: Boolean,
        isReferralAvailable: Boolean,
        isManageTokensAvailable: Boolean,
        onLinkMoreCardsClick: () -> Unit,
        onReferralClick: () -> Unit,
        onManageTokensClick: () -> Unit,
        onBackupClick: () -> Unit,
        onCardSettingsClick: () -> Unit,
    ) = WalletSettingsItemUM.WithItems(
        id = "card",
        description = resourceReference(R.string.settings_card_settings_footer),
        blocks = buildList {
            val isHotWallet = userWallet is UserWallet.Hot
            if (isHotWallet) {
                val hasBackup = userWallet.backedUp
                val backupBlock = BlockUM(
                    text = resourceReference(R.string.common_backup),
                    iconRes = R.drawable.ic_more_cards_24,
                    onClick = { onBackupClick() },
                    endContent = if (hasBackup) {
                        BlockUM.EndContent.None
                    } else {
                        BlockUM.EndContent.Label(
                            label = LabelUM(
                                text = resourceReference(R.string.hw_backup_no_backup),
                                style = LabelStyle.WARNING,
                            ),
                        )
                    },
                )

                add(backupBlock)
            }

            if (isManageTokensAvailable) {
                val manageTokensBlock = BlockUM(
                    text = resourceReference(R.string.add_tokens_title),
                    iconRes = R.drawable.ic_tether_24,
                    onClick = onManageTokensClick,
                )

                add(manageTokensBlock)
            }

            if (isLinkMoreCardsAvailable) {
                val linkMoreCardsBlock = BlockUM(
                    text = resourceReference(R.string.details_row_title_create_backup),
                    iconRes = R.drawable.ic_more_cards_24,
                    onClick = onLinkMoreCardsClick,
                )

                add(linkMoreCardsBlock)
            }

            if (!isHotWallet) {
                val deviceSettingsBlock = BlockUM(
                    text = resourceReference(R.string.card_settings_title),
                    iconRes = R.drawable.ic_card_settings_24,
                    onClick = { onCardSettingsClick() },
                )

                add(deviceSettingsBlock)
            }

            if (isReferralAvailable) {
                val referralBlock = BlockUM(
                    text = resourceReference(R.string.details_referral_title),
                    iconRes = R.drawable.ic_add_friends_24,
                    onClick = onReferralClick,
                )

                add(referralBlock)
            }
        }.toImmutableList(),
    )

    private fun buildForgetItem(forgetWallet: () -> Unit) = WalletSettingsItemUM.WithItems(
        id = "forget",
        description = resourceReference(R.string.settings_forget_wallet_footer),
        blocks = persistentListOf(
            BlockUM(
                text = resourceReference(R.string.settings_forget_wallet),
                iconRes = R.drawable.ic_card_foget_24,
                onClick = forgetWallet,
                accentType = BlockUM.AccentType.WARNING,
            ),
        ),
    )

    private fun buildAccessCodeItem(userWallet: UserWallet, onItemClick: () -> Unit): List<WalletSettingsItemUM> {
        return when (userWallet) {
            is UserWallet.Cold -> emptyList()
            is UserWallet.Hot -> buildHotWalletAccessCodeItem(userWallet, onItemClick)
        }
    }

    private fun buildHotWalletAccessCodeItem(
        userWallet: UserWallet.Hot,
        onItemClick: () -> Unit,
    ): List<WalletSettingsItemUM> {
        val isCodeSet = userWallet.hotWalletId.authType != HotWalletId.AuthType.NoPassword
        return listOf(
            WalletSettingsItemUM.WithItems(
                id = "access_code",
                description = resourceReference(R.string.wallet_settings_access_code_description),
                blocks = persistentListOf(
                    BlockUM(
                        text = if (isCodeSet) {
                            resourceReference(R.string.wallet_settings_change_access_code_title)
                        } else {
                            resourceReference(R.string.wallet_settings_set_access_code_title)
                        },
                        iconRes = R.drawable.ic_lock_24,
                        onClick = onItemClick,
                        accentType = BlockUM.AccentType.ACCENT,
                    ),
                ),
            ),
        )
    }
}