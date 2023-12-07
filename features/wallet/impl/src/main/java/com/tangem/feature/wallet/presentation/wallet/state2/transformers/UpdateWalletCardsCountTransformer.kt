package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.domain.getCardsCount
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import timber.log.Timber

internal class UpdateWalletCardsCountTransformer(
    private val userWallet: UserWallet,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(walletCardState = prevState.walletCardState.toUpdatedState())
            }
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(walletCardState = prevState.walletCardState.toUpdatedState())
            }
            is WalletState.Visa.Content -> {
                prevState.copy(walletCardState = prevState.walletCardState.toUpdatedState())
            }
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            -> {
                Timber.e("Impossible to update wallet cards count for locked wallet")
                prevState
            }
        }
    }

    private fun WalletCardState.toUpdatedState(): WalletCardState {
        return when (this) {
            is WalletCardState.Content -> copy(
                additionalInfo = WalletAdditionalInfoFactory.resolve(wallet = userWallet),
                imageResId = WalletImageResolver.resolve(userWallet = userWallet),
                cardCount = userWallet.getCardsCount(),
            )
            else -> this
        }
    }
}