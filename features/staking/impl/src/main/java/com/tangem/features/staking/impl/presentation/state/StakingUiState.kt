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
    val initialInfoState: StakingStates.InitialInfoState? = null,
    val amountState: StakingStates.AmountState? = null,
    val validatorState: StakingStates.ValidatorState? = null,
    val editAmountState: StakingStates.AmountState? = null,
    val editFeeState: StakingStates.FeeState? = null,
    val editValidatorState: StakingStates.ValidatorState? = null,
    val confirmStakingState: StakingStates.ConfirmStakingState? = null,
    val isBalanceHidden: Boolean,
    val isSubtracted: Boolean,
    val event: StateEvent<StakingEvent>,
) {

    fun getAmountState(isEditState: Boolean): StakingStates.AmountState? {
        return if (isEditState) {
            editAmountState
        } else {
            amountState
        }
    }

    fun getValidatorState(isEditState: Boolean): StakingStates.ValidatorState? {
        return if (isEditState) {
            editValidatorState
        } else {
            validatorState
        }
    }

    fun copyWrapped(
        initialInfoState: StakingStates.InitialInfoState? = this.initialInfoState,
        amountState: StakingStates.AmountState? = this.amountState,
        validatorState: StakingStates.ValidatorState? = this.validatorState,
        confirmStakingState: StakingStates.ConfirmStakingState? = this.confirmStakingState,
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
    @Stable
    data class InitialInfoState(
        override val type: StakingUiStateType = StakingUiStateType.InitialInfo,
        override val isPrimaryButtonEnabled: Boolean,
    ) : StakingStates()

    /** Amount state */
    @Stable
    data class AmountState(
        override val type: StakingUiStateType = StakingUiStateType.Amount,
        override val isPrimaryButtonEnabled: Boolean,
    ) : StakingStates()

    /** Validator state */
    @Stable
    data class ValidatorState(
        override val type: StakingUiStateType = StakingUiStateType.ValidatorAndFee,
        override val isPrimaryButtonEnabled: Boolean = false,
    ) : StakingStates()

    /** Fee state */
    @Stable
    data class FeeState(
        override val type: StakingUiStateType = StakingUiStateType.EditFee,
        override val isPrimaryButtonEnabled: Boolean,
    ) : StakingStates()

    /** Confirm state */
    @Stable
    data class ConfirmStakingState(
        override val type: StakingUiStateType = StakingUiStateType.Confirm,
        override val isPrimaryButtonEnabled: Boolean = false,
    ) : StakingStates()
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
