package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNFTItemUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM

internal class RemoveNFTCollectionsTransformer(
    userWalletId: UserWalletId,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState = when (prevState) {
        is WalletState.MultiCurrency.Content -> prevState.copy(
            nftState = WalletNFTItemUM.Hidden,
        )
        is WalletState.SingleCurrency.Content,
        is WalletState.MultiCurrency.Locked,
        is WalletState.SingleCurrency.Locked,
        -> prevState
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return when (walletUM) {
            is WalletUM.Content -> walletUM.copy(
                nftState = WalletNFTItemUM.Hidden,
            )
            is WalletUM.Locked -> walletUM
        }
    }
}