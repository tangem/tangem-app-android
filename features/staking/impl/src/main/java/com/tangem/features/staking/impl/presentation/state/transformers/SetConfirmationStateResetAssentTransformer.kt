package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal object SetConfirmationStateResetAssentTransformer : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmationState = prevState.confirmationState
        return prevState.copy(
            confirmationState = if (confirmationState is StakingStates.ConfirmationState.Data) {
                confirmationState.copy(
                    isPrimaryButtonEnabled = true,
                    innerState = InnerConfirmationStakingState.ASSENT,
                )
            } else {
                confirmationState
            },
        )
    }
}