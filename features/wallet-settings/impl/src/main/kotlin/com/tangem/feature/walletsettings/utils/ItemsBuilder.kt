package com.tangem.feature.walletsettings.utils

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.ManageTokens.Source
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.walletsettings.analytics.Settings
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.impl.R
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
        userWalletId: UserWalletId,
        userWalletName: String,
        isLinkMoreCardsAvailable: Boolean,
        isReferralAvailable: Boolean,
        isManageTokensAvailable: Boolean,
        isRenameWalletAvailable: Boolean,
        isNFTFeatureEnabled: Boolean,
        isNFTEnabled: Boolean,
        onCheckedNFTChange: (Boolean) -> Unit,
        isNotificationsFeatureEnabled: Boolean,
        isNotificationsEnabled: Boolean,
        onCheckedNotificationsChanged: (Boolean) -> Unit,
        onNotificationsDescriptionClick: () -> Unit,
        forgetWallet: () -> Unit,
        renameWallet: () -> Unit,
        onLinkMoreCardsClick: () -> Unit,
        onReferralClick: () -> Unit,
    ): PersistentList<WalletSettingsItemUM> = persistentListOf<WalletSettingsItemUM>()
        .add(buildNameItem(userWalletName, isRenameWalletAvailable, renameWallet))
        .run {
            if (isNFTFeatureEnabled) {
                add(buildNFTItem(isNFTEnabled, onCheckedNFTChange))
            } else {
                this
            }
        }
        .add(
            buildCardItem(
                userWalletId = userWalletId,
                isLinkMoreCardsAvailable = isLinkMoreCardsAvailable,
                isReferralAvailable = isReferralAvailable,
                isManageTokensAvailable = isManageTokensAvailable,
                onLinkMoreCardsClick = onLinkMoreCardsClick,
                onReferralClick = onReferralClick,
            ),
        )
        .run {
            if (isNotificationsFeatureEnabled) {
                add(
                    buildNotificationsSwitchItem(
                        isNotificationsEnabled,
                        onCheckedNotificationsChanged,
                    ),
                ).add(
                    buildNotificationsDescriptionItem(
                        onDescriptionClick = onNotificationsDescriptionClick,
                    ),
                )
            } else {
                this
            }
        }
        .add(buildForgetItem(forgetWallet))

    private fun buildNameItem(walletName: String, isRenameWalletAvailable: Boolean, renameWallet: () -> Unit) =
        WalletSettingsItemUM.WithText(
            id = "wallet_name",
            title = resourceReference(id = R.string.settings_wallet_name_title),
            text = stringReference(walletName),
            isEnabled = isRenameWalletAvailable,
            onClick = renameWallet,
        )

    private fun buildNFTItem(isNFTEnabled: Boolean, onCheckedNFTChange: (Boolean) -> Unit) =
        WalletSettingsItemUM.WithSwitch(
            id = "nft",
            title = resourceReference(id = R.string.details_nft_title),
            isChecked = isNFTEnabled,
            onCheckedChange = onCheckedNFTChange,
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
        userWalletId: UserWalletId,
        isLinkMoreCardsAvailable: Boolean,
        isReferralAvailable: Boolean,
        isManageTokensAvailable: Boolean,
        onLinkMoreCardsClick: () -> Unit,
        onReferralClick: () -> Unit,
    ) = WalletSettingsItemUM.WithItems(
        id = "card",
        description = resourceReference(R.string.settings_card_settings_footer),
        blocks = buildList {
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

            BlockUM(
                text = resourceReference(R.string.card_settings_title),
                iconRes = R.drawable.ic_card_settings_24,
                onClick = { router.push(AppRoute.CardSettings(userWalletId)) },
            ).let(::add)

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
}