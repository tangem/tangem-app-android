package com.tangem.features.jointaccount.creation.config.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.utils.transformer.Transformer

internal class UpdateWalletsTransformer(
    private val wallets: List<UserWallet>,
    private val selectedWallet: UserWallet?,
    private val onWalletRowClick: () -> Unit,
) : Transformer<JointAccountConfigUM> {

    override fun transform(prevState: JointAccountConfigUM): JointAccountConfigUM = prevState.copy(
        // A single wallet leaves nothing to choose, so the row is not shown at all
        wallet = if (wallets.size > 1 && selectedWallet != null) {
            JointAccountConfigUM.WalletUM(name = selectedWallet.name, onClick = onWalletRowClick)
        } else {
            null
        },
    )
}