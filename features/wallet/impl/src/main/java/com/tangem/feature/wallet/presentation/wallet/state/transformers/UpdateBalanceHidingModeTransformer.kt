package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState

internal class UpdateBalanceHidingModeTransformer(
    private val isHidingMode: Boolean,
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(isHidingMode = isHidingMode)
    }
}