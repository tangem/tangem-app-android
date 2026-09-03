package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.utils.transformer.Transformer

@Suppress("LongParameterList")
internal class SetContentTransformer(
    private val preview: JointAccountInvitePreview,
    private val wallets: List<UserWallet>,
    private val selectedWallet: UserWallet?,
    private val onCreatorInfoClick: () -> Unit,
    private val onContinueClick: () -> Unit,
    private val onWalletRowClick: () -> Unit,
    private val onCloseClick: () -> Unit,
) : Transformer<JointAccountInvitePreviewUM> {

    override fun transform(prevState: JointAccountInvitePreviewUM): JointAccountInvitePreviewUM {
        return JointAccountInvitePreviewUM.Content(
            accountName = preview.config.name,
            accountIcon = AccountIconUM.CryptoPortfolio(
                value = preview.config.icon.toAccountIcon(),
                color = preview.config.iconColor.toAccountIconColor(),
            ),
            requiredToSign = preview.config.threshold,
            totalMembers = preview.config.membersCount,
            creatorName = preview.creator.name,
            creatorAddress = preview.creator.address,
            wallet = resolveWalletRow(wallets, selectedWallet, onWalletRowClick),
            confirmation = null,
            onCreatorInfoClick = onCreatorInfoClick,
            onContinueClick = onContinueClick,
            onCloseClick = onCloseClick,
        )
    }
}

/** Falls back to a default rather than crashing on an icon name this app build does not know yet. */
private fun String.toAccountIcon(): CryptoPortfolioIcon.Icon =
    CryptoPortfolioIcon.Icon.entries.find { it.name == this } ?: CryptoPortfolioIcon.Icon.Letter

private fun String.toAccountIconColor(): CryptoPortfolioIcon.Color =
    CryptoPortfolioIcon.Color.entries.find { it.name == this } ?: CryptoPortfolioIcon.Color.Azure