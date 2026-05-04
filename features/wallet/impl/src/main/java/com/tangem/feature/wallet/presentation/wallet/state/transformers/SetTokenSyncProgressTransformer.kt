package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.state.model.TokenSyncProgressUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM

internal class SetTokenSyncProgressTransformer(
    private val userWallet: UserWallet,
    private val progress: TokenSyncProgressUM,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> prevState.copy(
                walletCardState = updateCardState(prevState.walletCardState),
                tokenSyncProgressUM = progress,
            )
            else -> prevState
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM = walletUM

    private fun updateCardState(cardState: WalletCardState): WalletCardState {
        val additionalInfo = WalletAdditionalInfoFactory.resolve(wallet = userWallet, syncProgress = progress)
        return when (cardState) {
            is WalletCardState.Loading -> cardState.copy(additionalInfo = additionalInfo)
            is WalletCardState.Content -> cardState.copy(additionalInfo = additionalInfo)
            else -> cardState
        }
    }
}