package com.tangem.features.staking.impl.presentation.state.transformers.validator

import com.tangem.domain.staking.model.Yield
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import com.tangem.utils.transformer.Transformer

internal class ValidatorSelectChangeTransformer(
    private val selectedValidator: Yield.Validator,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmState = prevState.confirmStakingState as? StakingStates.ConfirmStakingState.Data ?: return prevState
        val validatorState = confirmState.validatorState as? ValidatorState.Content ?: return prevState

        return prevState.copy(
            confirmStakingState = confirmState.copy(
                validatorState = validatorState.copy(
                    chosenValidator = selectedValidator,
                ),
            ),
        )
    }
}