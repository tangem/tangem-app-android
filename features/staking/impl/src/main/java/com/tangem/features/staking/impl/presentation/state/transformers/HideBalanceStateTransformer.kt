package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class HideBalanceStateTransformer(
    private val isBalanceHidden: Boolean,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(isBalanceHidden = isBalanceHidden)
    }
}
