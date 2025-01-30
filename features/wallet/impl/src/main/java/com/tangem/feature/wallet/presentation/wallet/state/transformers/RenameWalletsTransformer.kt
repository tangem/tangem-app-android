package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/**
 * Transformer that renames wallets
 *
 * @property renamedWallets renamed wallets
 */
internal class RenameWalletsTransformer(
    private val renamedWallets: List<UserWallet>,
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            wallets = prevState.wallets.map { walletState ->
                val renamedWallet = renamedWallets.firstOrNull { it.walletId == walletState.walletCardState.id }

                if (renamedWallet != null) {
                    transform(prevState = walletState, newName = renamedWallet.name)
                } else {
                    walletState
                }
            }
                .toImmutableList(),
        )
    }

    private fun transform(prevState: WalletState, newName: String): WalletState {
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