package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import timber.log.Timber

internal class RenameWalletTransformer(
    userWalletId: UserWalletId,
    private val newName: String,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(walletCardState = prevState.walletCardState.copySealed(title = newName))
            }
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(walletCardState = prevState.walletCardState.copySealed(title = newName))
            }
            is WalletState.Visa.Content -> {
                prevState.copy(walletCardState = prevState.walletCardState.copySealed(title = newName))
            }
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            -> {
                Timber.e("Impossible to rename wallet in locked state")
                prevState
            }
        }
    }
}