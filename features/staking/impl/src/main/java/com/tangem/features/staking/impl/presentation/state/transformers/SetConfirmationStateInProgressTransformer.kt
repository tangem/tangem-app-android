package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class SetConfirmationStateInProgressTransformer : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            confirmationState = prevState.confirmationState.copyWrapped(),
        )
    }

    private fun StakingStates.ConfirmationState.copyWrapped(): StakingStates.ConfirmationState {
        return if (this is StakingStates.ConfirmationState.Data) {
            copy(
                isPrimaryButtonEnabled = false,
                innerState = InnerConfirmationStakingState.IN_PROGRESS,
                validatorState = validatorState.copySealed(isClickable = false),
            )
        } else {
            this
        }
    }
}
