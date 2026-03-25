package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.toImmutableList

internal abstract class WalletStateTransformer(
    protected val userWalletId: UserWalletId,
) : WalletScreenStateTransformer {

    abstract fun transform(prevState: WalletState): WalletState

    abstract fun transform(walletUM: WalletUM): WalletUM

    final override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            wallets = prevState.wallets
                .map { state ->
                    if (state.walletCardState.id == userWalletId) transform(state) else state
                }
                .toImmutableList(),
            wallets2 = prevState.wallets2
                .map { walletUM ->
                    if (walletUM.walletsBalanceUM.id == userWalletId) transform(walletUM) else walletUM
                }
                .toImmutableList(),
        )
    }
}