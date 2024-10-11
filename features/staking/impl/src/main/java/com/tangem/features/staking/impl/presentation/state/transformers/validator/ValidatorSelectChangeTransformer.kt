package com.tangem.features.staking.impl.presentation.state.transformers.validator

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class ValidatorSelectChangeTransformer(
    private val selectedValidator: Yield.Validator?,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        selectedValidator ?: return prevState
        val validatorState = (prevState.validatorState as? StakingStates.ValidatorState.Data)?.copy(
            chosenValidator = selectedValidator,
        ) ?: StakingStates.ValidatorState.Data(
            isClickable = false,
            availableValidators = emptyList(),
            chosenValidator = selectedValidator,
            isPrimaryButtonEnabled = true,
        )

        return prevState.copy(
            validatorState = validatorState,
        )
    }
}
