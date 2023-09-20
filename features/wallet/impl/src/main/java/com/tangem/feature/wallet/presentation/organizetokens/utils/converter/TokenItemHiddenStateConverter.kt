package com.tangem.feature.wallet.presentation.organizetokens.utils.converter

import com.tangem.feature.wallet.presentation.common.state.TokenItemState

internal class TokenItemHiddenStateConverter {

    fun updateHiddenState(
        optionsState: TokenItemState.TokenOptionsState,
        isBalanceHidden: Boolean,
    ): TokenItemState.TokenOptionsState {
        return when {
            !optionsState.isBalanceHidden && isBalanceHidden -> {
                optionsState.copy(isBalanceHidden = true)
            }
            optionsState.isBalanceHidden && !isBalanceHidden -> {
                optionsState.copy(isBalanceHidden = false)
            }
            else -> optionsState
        }
    }
}