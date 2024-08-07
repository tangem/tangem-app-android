package com.tangem.feature.walletsettings.utils

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.impl.R
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@ComponentScoped
internal class ItemsBuilder @Inject constructor(
    private val router: Router,
) {

    @Suppress("LongParameterList")
    fun buildItems(
        userWalletId: UserWalletId,
        userWalletName: String,
        isLinkMoreCardsAvailable: Boolean,
        isReferralAvailable: Boolean,
        forgetWallet: () -> Unit,
        renameWallet: () -> Unit,
        onLinkMoreCardsClick: () -> Unit,
    ): PersistentList<WalletSettingsItemUM> = persistentListOf(
        buildNameItem(userWalletName, renameWallet),
        buildCardItem(userWalletId, isLinkMoreCardsAvailable, isReferralAvailable, onLinkMoreCardsClick),
        buildForgetItem(forgetWallet),
    )

    private fun buildNameItem(walletName: String, renameWallet: () -> Unit) = WalletSettingsItemUM.WithText(
        id = "wallet_name",
        title = resourceReference(id = R.string.settings_wallet_name_title),
        text = stringReference(walletName),
        onClick = renameWallet,
    )

    private fun buildCardItem(
        userWalletId: UserWalletId,
        isLinkMoreCardsAvailable: Boolean,
        isReferralAvailable: Boolean,
        onLinkMoreCardsClick: () -> Unit,
    ) = WalletSettingsItemUM.WithItems(
        id = "card",
        description = resourceReference(R.string.settings_card_settings_footer),
        blocks = buildList {
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
                    onClick = { router.push(AppRoute.ReferralProgram(userWalletId)) },
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
