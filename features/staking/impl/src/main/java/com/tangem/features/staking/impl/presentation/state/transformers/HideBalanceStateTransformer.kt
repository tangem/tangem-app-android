package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.features.staking.impl.presentation.state.StakingUiState

internal class HideBalanceStateTransformer(
    private val isBalanceHidden: Boolean,
) : StakingScreenStateTransformer {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(isBalanceHidden = isBalanceHidden)
    }
}