package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.domain.staking.model.Yield

@Immutable
internal sealed class InnerValidatorState {

    data class Content(
        val chosenValidator: Yield.Validator,
    ) : InnerValidatorState()

    data object Loading : InnerValidatorState()

    data object Error : InnerValidatorState()
}