package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
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
            }.toImmutableList(),
            wallets2 = prevState.wallets2.map { walletUM ->
                val renamedWallet = renamedWallets.firstOrNull { it.walletId == walletUM.walletsBalanceUM.id }

                if (renamedWallet != null) {
                    transform(prevState = walletUM, newName = renamedWallet.name)
                } else {
                    walletUM
                }
            }.toPersistentList(),
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
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            -> {
                Timber.e("Impossible to rename wallet in locked state")
                prevState
            }
        }
    }

    private fun transform(prevState: WalletUM, newName: String): WalletUM {
        return when (prevState) {
            is WalletUM.Content -> {
                prevState.copy(walletsBalanceUM = prevState.walletsBalanceUM.copySealed(name = newName))
            }
            is WalletUM.Locked -> {
                Timber.e("Impossible to rename wallet in locked state")
                prevState
            }
        }
    }
}