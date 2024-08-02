package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.domain.staking.model.stakekit.Yield

@Immutable
internal sealed class ValidatorState {

    abstract val isClickable: Boolean

    data class Content(
        override val isClickable: Boolean,
        val chosenValidator: Yield.Validator,
        val availableValidators: List<Yield.Validator>,
    ) : ValidatorState()

    data object Loading : ValidatorState() {
        override val isClickable: Boolean
            get() = false
    }

    data object Error : ValidatorState() {
        override val isClickable: Boolean
            get() = false
    }

    fun copySealed(isClickable: Boolean): ValidatorState {
        return if (this is Content) {
            copy(isClickable = isClickable)
        } else {
            this
        }
    }
}
