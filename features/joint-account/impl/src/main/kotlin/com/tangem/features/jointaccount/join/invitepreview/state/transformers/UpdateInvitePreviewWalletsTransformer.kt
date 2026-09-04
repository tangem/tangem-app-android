package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.utils.transformer.Transformer

internal class UpdateInvitePreviewWalletsTransformer(
    private val wallets: List<UserWallet>,
    private val selectedWallet: UserWallet?,
    private val onWalletRowClick: () -> Unit,
) : Transformer<JointAccountInvitePreviewUM> {

    override fun transform(prevState: JointAccountInvitePreviewUM): JointAccountInvitePreviewUM {
        if (prevState !is JointAccountInvitePreviewUM.Content) return prevState

        return prevState.copy(wallet = resolveWalletRow(wallets, selectedWallet, onWalletRowClick))
    }
}

/** A single wallet leaves nothing to choose, so the row is not shown at all. */
internal fun resolveWalletRow(
    wallets: List<UserWallet>,
    selectedWallet: UserWallet?,
    onWalletRowClick: () -> Unit,
): JointAccountInvitePreviewUM.Content.WalletUM? {
    return if (wallets.size > 1 && selectedWallet != null) {
        JointAccountInvitePreviewUM.Content.WalletUM(name = selectedWallet.name, onClick = onWalletRowClick)
    } else {
        null
    }
}