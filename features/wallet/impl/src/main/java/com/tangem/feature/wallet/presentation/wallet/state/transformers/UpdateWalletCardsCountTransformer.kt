package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import timber.log.Timber

internal class UpdateWalletCardsCountTransformer(
    private val userWallet: UserWallet,
    private val walletImageResolver: WalletImageResolver,
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
            is WalletState.Visa.AccessTokenLocked,
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
                imageResId = walletImageResolver.resolve(userWallet = userWallet),
                cardCount = userWallet.getCardsCount(),
            )
            else -> this
        }
    }
}