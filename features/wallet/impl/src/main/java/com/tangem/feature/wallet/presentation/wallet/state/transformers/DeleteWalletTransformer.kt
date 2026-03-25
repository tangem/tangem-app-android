package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

internal class DeleteWalletTransformer(
    private val selectedWalletIndex: Int,
    private val deletedWalletId: UserWalletId,
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        val deletedWalletState = prevState.getDeletedWalletState()
        val deletedWalletUM = prevState.getDeletedWalletState2()

        return when {
            deletedWalletUM != null && deletedWalletState != null -> prevState.copy(
                selectedWalletIndex = selectedWalletIndex,
                wallets = (prevState.wallets - deletedWalletState).toImmutableList(),
                wallets2 = (prevState.wallets2 - deletedWalletUM).toImmutableList(),
            )
            deletedWalletUM != null -> prevState.copy(
                selectedWalletIndex = selectedWalletIndex,
                wallets2 = (prevState.wallets2 - deletedWalletUM).toImmutableList(),
            )
            deletedWalletState != null -> prevState.copy(
                selectedWalletIndex = selectedWalletIndex,
                wallets = (prevState.wallets - deletedWalletState).toImmutableList(),
            )
            else -> {
                Timber.e("Wallets does not contain deleted wallet")
                prevState
            }
        }
    }

    private fun WalletScreenState.getDeletedWalletState(): WalletState? {
        return wallets.firstOrNull { it.walletCardState.id == deletedWalletId }
    }

    private fun WalletScreenState.getDeletedWalletState2(): WalletUM? {
        return wallets2.firstOrNull { it.walletsBalanceUM.id == deletedWalletId }
    }
}