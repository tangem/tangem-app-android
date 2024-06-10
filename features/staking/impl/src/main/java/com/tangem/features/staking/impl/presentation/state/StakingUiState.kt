package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.event.StateEvent
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents

/**
 * Ui states of the staking screen
 */
@Immutable
internal data class StakingUiState(
    val clickIntents: StakingClickIntents,
    val cryptoCurrencyName: String,
    val currentScreen: StakingUiStateType,
    val initialInfoState: StakingStates.InitialInfoState,
    val amountState: StakingStates.AmountState,
    val validatorState: StakingStates.ValidatorState,
    val confirmStakingState: StakingStates.ConfirmStakingState,
    val isBalanceHidden: Boolean,
    val event: StateEvent<StakingEvent>,
) {

    fun copyWrapped(
        initialInfoState: StakingStates.InitialInfoState = this.initialInfoState,
        amountState: StakingStates.AmountState = this.amountState,
        validatorState: StakingStates.ValidatorState = this.validatorState,
        confirmStakingState: StakingStates.ConfirmStakingState = this.confirmStakingState,
    ): StakingUiState = copy(
        initialInfoState = initialInfoState,
        amountState = amountState,
        validatorState = validatorState,
        confirmStakingState = confirmStakingState,
    )
}

internal sealed class StakingStates {

    abstract val isPrimaryButtonEnabled: Boolean

    /** Initial info state */
    sealed class InitialInfoState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
        ) : InitialInfoState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : InitialInfoState()
    }

    /** Amount state */
    sealed class AmountState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
        ) : AmountState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : AmountState()
    }

    /** Validator state */
    sealed class ValidatorState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
        ) : ValidatorState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : ValidatorState()
    }

    /** Fee state */
    sealed class FeeState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
        ) : FeeState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : FeeState()
    }

    /** Confirm state */
    sealed class ConfirmStakingState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
        ) : ConfirmStakingState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : ConfirmStakingState()
    }
}

enum class StakingUiStateType {
    InitialInfo,
    Amount,
    ValidatorAndFee,
    Confirm,
}