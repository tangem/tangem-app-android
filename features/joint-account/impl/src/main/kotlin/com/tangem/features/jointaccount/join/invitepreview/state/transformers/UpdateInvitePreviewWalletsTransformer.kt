package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.utils.transformer.Transformer

internal class UpdateInvitePreviewWalletsTransformer(
    private val wallets: List<UserWallet>,
    private val selectedWallet: UserWallet?,
    private val onWalletRowClick: () -> Unit,
) : Transformer<JointAccountInvitePreviewUM> {

    override fun transform(prevState: JointAccountInvitePreviewUM): JointAccountInvitePreviewUM = prevState.copy(
        // A single wallet leaves nothing to choose, so the row is not shown at all
        wallet = if (wallets.size > 1 && selectedWallet != null) {
            JointAccountInvitePreviewUM.WalletUM(name = selectedWallet.name, onClick = onWalletRowClick)
        } else {
            null
        },
    )
}