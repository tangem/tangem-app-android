package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

internal class DeleteWalletTransformer(
    private val selectedWalletIndex: Int,
    private val deletedWalletId: UserWalletId,
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        val deletedWalletState = prevState.getDeletedWalletState()

        if (deletedWalletState == null) {
            Timber.e("Wallets does not contain deleted wallet")
            return prevState
        }

        return prevState.copy(
            selectedWalletIndex = selectedWalletIndex,
            wallets = (prevState.wallets - deletedWalletState).toImmutableList(),
        )
    }

    private fun WalletScreenState.getDeletedWalletState(): WalletState? {
        return wallets.firstOrNull { it.walletCardState.id == deletedWalletId }
    }
}