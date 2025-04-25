package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNFTItemUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState

internal class RemoveNFTCollectionsTransformer(
    userWalletId: UserWalletId,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState = when (prevState) {
        is WalletState.MultiCurrency.Content -> prevState.copy(
            nftState = WalletNFTItemUM.Hidden,
        )
        is WalletState.SingleCurrency.Content,
        is WalletState.Visa.Content,
        is WalletState.MultiCurrency.Locked,
        is WalletState.SingleCurrency.Locked,
        is WalletState.Visa.Locked,
        is WalletState.Visa.AccessTokenLocked,
        -> prevState
    }
}