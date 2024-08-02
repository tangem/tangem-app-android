package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import kotlinx.collections.immutable.toImmutableList

internal abstract class WalletStateTransformer(
    protected val userWalletId: UserWalletId,
) : WalletScreenStateTransformer {

    abstract fun transform(prevState: WalletState): WalletState

    final override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            wallets = prevState.wallets
                .map { state ->
                    if (state.walletCardState.id == userWalletId) transform(state) else state
                }
                .toImmutableList(),
        )
    }
}
