package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal object SetConfirmationStateEmptyTransformer : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            actionType = StakingActionCommonType.Enter(skipEnterAmount = false),
            validatorState = StakingStates.ValidatorState.Empty(),
            confirmationState = StakingStates.ConfirmationState.Empty(),
            balanceState = null,
        )
    }
}