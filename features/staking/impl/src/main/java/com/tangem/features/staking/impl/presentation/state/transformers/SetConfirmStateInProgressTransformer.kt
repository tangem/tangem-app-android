package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.features.staking.impl.presentation.state.InnerConfirmStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class SetConfirmStateInProgressTransformer : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            confirmStakingState = prevState.confirmStakingState.copyWrapped(),
        )
    }

    private fun StakingStates.ConfirmStakingState.copyWrapped(): StakingStates.ConfirmStakingState {
        return if (this is StakingStates.ConfirmStakingState.Data) {
            copy(
                isPrimaryButtonEnabled = false,
                innerState = InnerConfirmStakingState.IN_PROGRESS,
                validatorState = validatorState.copySealed(isClickable = false),
            )
        } else {
            this
        }
    }
}
