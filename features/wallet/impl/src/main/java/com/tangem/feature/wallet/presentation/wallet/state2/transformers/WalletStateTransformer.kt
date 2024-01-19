package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

internal abstract class WalletStateTransformer(
    protected val userWalletId: UserWalletId,
) : WalletScreenStateTransformer {

    abstract fun transform(prevState: WalletState): WalletState

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            wallets = prevState.wallets
                .map { state ->
                    if (state.walletCardState.id == userWalletId) transform(state) else state
                }
                .toImmutableList(),
        )
    }

    protected inline fun <reified S : WalletState> WalletState.transformWhenInState(
        transform: (state: S) -> WalletState,
    ): WalletState = if (this is S) {
        transform(this)
    } else {
        Timber.w("Impossible to transform ${this::class.simpleName} because current is ${S::class.simpleName}")
        this
    }
}