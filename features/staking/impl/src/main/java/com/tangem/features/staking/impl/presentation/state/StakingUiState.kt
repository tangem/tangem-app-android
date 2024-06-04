package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.tangem.core.ui.event.StateEvent
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents

/**
 * Ui states of the staking screen
 */
@Immutable
internal data class StakingUiState(
    val clickIntents: StakingClickIntents,
    val cryptoCurrencyName: String,
    val initialInfoState: StakingStates.InitialInfoState,
    val amountState: StakingStates.AmountState,
    val validatorState: StakingStates.ValidatorState,
    val editAmountState: StakingStates.AmountState,
    val editFeeState: StakingStates.FeeState,
    val editValidatorState: StakingStates.ValidatorState,
    val confirmStakingState: StakingStates.ConfirmStakingState,
    val isBalanceHidden: Boolean,
    val isSubtracted: Boolean,
    val event: StateEvent<StakingEvent>,
) {

    fun getAmountState(isEditState: Boolean): StakingStates.AmountState {
        return if (isEditState) {
            editAmountState
        } else {
            amountState
        }
    }

    fun getValidatorState(isEditState: Boolean): StakingStates.ValidatorState {
        return if (isEditState) {
            editValidatorState
        } else {
            validatorState
        }
    }

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

@Stable
internal sealed class StakingStates {

    abstract val type: StakingUiStateType

    abstract val isPrimaryButtonEnabled: Boolean

    /** Initial info state */
    sealed class InitialInfoState : StakingStates() {
        @Stable
        data class Data(
            override val type: StakingUiStateType = StakingUiStateType.InitialInfo,
            override val isPrimaryButtonEnabled: Boolean,
        ) : InitialInfoState()

        @Stable
        data class Empty(
            override val type: StakingUiStateType = StakingUiStateType.InitialInfo,
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : InitialInfoState()
    }

    /** Amount state */
    @Stable
    sealed class AmountState : StakingStates() {
        @Stable
        data class Data(
            override val type: StakingUiStateType = StakingUiStateType.Amount,
            override val isPrimaryButtonEnabled: Boolean,
        ) : AmountState()

        @Stable
        data class Empty(
            override val type: StakingUiStateType = StakingUiStateType.Amount,
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : AmountState()
    }

    /** Validator state */
    @Stable
    sealed class ValidatorState : StakingStates() {
        @Stable
        data class Data(
            override val type: StakingUiStateType = StakingUiStateType.ValidatorAndFee,
            override val isPrimaryButtonEnabled: Boolean,
        ) : ValidatorState()

        @Stable
        data class Empty(
            override val type: StakingUiStateType = StakingUiStateType.ValidatorAndFee,
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : ValidatorState()
    }


    /** Fee state */
    @Stable
    sealed class FeeState : StakingStates() {
        @Stable
        data class Data(
            override val type: StakingUiStateType = StakingUiStateType.EditFee,
            override val isPrimaryButtonEnabled: Boolean,
        ) : FeeState()

        @Stable
        data class Empty(
            override val type: StakingUiStateType = StakingUiStateType.EditFee,
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : FeeState()
    }

    /** Confirm state */
    @Stable
    sealed class ConfirmStakingState : StakingStates() {
        @Stable
        data class Data(
            override val type: StakingUiStateType = StakingUiStateType.Confirm,
            override val isPrimaryButtonEnabled: Boolean,
        ) : ConfirmStakingState()

        @Stable
        data class Empty(
            override val type: StakingUiStateType = StakingUiStateType.EditFee,
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : ConfirmStakingState()
    }
}

data class StakingUiCurrentScreen(
    val type: StakingUiStateType,
    val isFromConfirmation: Boolean, // TODO staking
)

enum class StakingUiStateType {
    None,
    InitialInfo,
    Amount,
    ValidatorAndFee,
    Confirm,
    EditAmount,
    EditValidator,
    EditFee,
}
