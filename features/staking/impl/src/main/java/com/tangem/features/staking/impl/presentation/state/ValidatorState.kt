package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.domain.staking.model.Yield

@Immutable
internal sealed class ValidatorState {

    data class Content(
        val chosenValidator: Yield.Validator,
        val availableValidators: List<Yield.Validator>,
    ) : ValidatorState()

    data object Loading : ValidatorState()

    data object Error : ValidatorState()
}