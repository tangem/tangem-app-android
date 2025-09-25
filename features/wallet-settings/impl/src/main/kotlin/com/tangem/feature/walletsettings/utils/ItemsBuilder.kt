package com.tangem.feature.walletsettings.utils

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.ManageTokens.Source
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.walletsettings.analytics.Settings
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.hot.sdk.model.HotWalletId
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@ModelScoped
internal class ItemsBuilder @Inject constructor(
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

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
        isNotificationsFeatureEnabled: Boolean,
        isNotificationsEnabled: Boolean,
        isNotificationsPermissionGranted: Boolean,
        onCheckedNotificationsChanged: (Boolean) -> Unit,
        onNotificationsDescriptionClick: () -> Unit,
        forgetWallet: () -> Unit,
        onLinkMoreCardsClick: () -> Unit,
        onReferralClick: () -> Unit,
        onAccessCodeClick: () -> Unit,
        walletUpgradeDismissed: Boolean,
        onUpgradeWalletClick: () -> Unit,
        onDismissUpgradeWalletClick: () -> Unit,
    ): PersistentList<WalletSettingsItemUM> = persistentListOf<WalletSettingsItemUM>()
        .add(cardItem)
        .addAll(
            buildUpgradeWalletItem(
                userWallet = userWallet,
                walletUpgradeDismissed = walletUpgradeDismissed,
                onUpgradeWalletClick = onUpgradeWalletClick,
                onDismissUpgradeWalletClick = onDismissUpgradeWalletClick,
            ),
        )
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
            ),
        )
        .addAll(
            buildNotificationItems(
                isNotificationsFeatureEnabled = isNotificationsFeatureEnabled,
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
        isNotificationsFeatureEnabled: Boolean,
        isNotificationsPermissionGranted: Boolean,
        isNotificationsEnabled: Boolean,
        onCheckedNotificationsChanged: (Boolean) -> Unit,
        onNotificationsDescriptionClick: () -> Unit,
    ): List<WalletSettingsItemUM> {
        if (!isNotificationsFeatureEnabled) return emptyList()
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

    private fun buildUpgradeWalletItem(
        userWallet: UserWallet,
        walletUpgradeDismissed: Boolean,
        onUpgradeWalletClick: () -> Unit,
        onDismissUpgradeWalletClick: () -> Unit,
    ): List<WalletSettingsItemUM> = when (userWallet) {
        is UserWallet.Cold -> emptyList()
        is UserWallet.Hot -> if (!walletUpgradeDismissed) {
            listOf(
                WalletSettingsItemUM.UpgradeWallet(
                    id = "upgrade_wallet",
                    title = resourceReference(id = R.string.hw_upgrade_to_cold_banner_title),
                    description = resourceReference(id = R.string.hw_upgrade_to_cold_banner_description),
                    onClick = onUpgradeWalletClick,
                    onDismissClick = onDismissUpgradeWalletClick,
                ),
            )
        } else {
            emptyList()
        }
    }

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
    ) = WalletSettingsItemUM.WithItems(
        id = "card",
        description = resourceReference(R.string.settings_card_settings_footer),
        blocks = buildList {
            val userWalletId = userWallet.walletId
            val isHotWallet = userWallet is UserWallet.Hot
            if (isHotWallet) {
                val hasBackup = userWallet.backedUp
                BlockUM(
                    text = resourceReference(R.string.common_backup),
                    iconRes = R.drawable.ic_more_cards_24,
                    onClick = { router.push(AppRoute.WalletBackup(userWalletId)) },
                    label = if (hasBackup) {
                        null
                    } else {
                        LabelUM(
                            text = resourceReference(R.string.hw_backup_no_backup),
                            style = LabelStyle.WARNING,
                        )
                    },
                ).let(::add)
            }

            if (isManageTokensAvailable) {
                BlockUM(
                    text = resourceReference(R.string.add_tokens_title),
                    iconRes = R.drawable.ic_tether_24,
                    onClick = {
                        analyticsEventHandler.send(Settings.ButtonManageTokens)
                        router.push(AppRoute.ManageTokens(Source.SETTINGS, userWalletId))
                    },
                ).let(::add)
            }

            if (isLinkMoreCardsAvailable) {
                BlockUM(
                    text = resourceReference(R.string.details_row_title_create_backup),
                    iconRes = R.drawable.ic_more_cards_24,
                    onClick = onLinkMoreCardsClick,
                ).let(::add)
            }

            if (!isHotWallet) {
                BlockUM(
                    text = resourceReference(R.string.card_settings_title),
                    iconRes = R.drawable.ic_card_settings_24,
                    onClick = { router.push(AppRoute.CardSettings(userWalletId)) },
                ).let(::add)
            }

            if (isReferralAvailable) {
                BlockUM(
                    text = resourceReference(R.string.details_referral_title),
                    iconRes = R.drawable.ic_add_friends_24,
                    onClick = onReferralClick,
                ).let(::add)
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