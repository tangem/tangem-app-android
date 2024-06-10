package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.features.staking.impl.presentation.state.StakingUiState

internal interface StakingScreenStateTransformer {

    fun transform(prevState: StakingUiState): StakingUiState
}