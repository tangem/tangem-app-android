package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal object AddStakingErrorTransformer : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmationState =
            prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState

        return prevState.copy(
            confirmationState = confirmationState.copy(
                feeState = FeeState.Error,
            ),
        )
    }
}