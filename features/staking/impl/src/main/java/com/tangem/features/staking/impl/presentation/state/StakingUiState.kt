package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.staking.impl.presentation.state.transformers.InfoType
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import kotlinx.collections.immutable.ImmutableList

/**
 * Ui states of the staking screen
 */
@Immutable
internal data class StakingUiState(
    val clickIntents: StakingClickIntents,
    val cryptoCurrencyName: String,
    val currentStep: StakingStep,
    val initialInfoState: StakingStates.InitialInfoState,
    val amountState: AmountState,
    val confirmStakingState: StakingStates.ConfirmStakingState,
    val isBalanceHidden: Boolean,
    val bottomSheetConfig: TangemBottomSheetConfig?,
    val event: StateEvent<StakingEvent>,
) {

    fun copyWrapped(
        initialInfoState: StakingStates.InitialInfoState = this.initialInfoState,
        amountState: AmountState = this.amountState,
        confirmStakingState: StakingStates.ConfirmStakingState = this.confirmStakingState,
    ): StakingUiState = copy(
        initialInfoState = initialInfoState,
        amountState = amountState,
        confirmStakingState = confirmStakingState,
    )
}

internal sealed class StakingStates {

    abstract val isPrimaryButtonEnabled: Boolean

    /** Initial info state */
    sealed class InitialInfoState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
            val available: String,
            val onStake: String,
            val aprRange: TextReference,
            val unbondingPeriod: String,
            val minimumRequirement: String,
            val rewardClaiming: String,
            val warmupPeriod: String,
            val rewardSchedule: String,
            val onInfoClick: (InfoType) -> Unit,
        ) : InitialInfoState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : InitialInfoState()
    }

    /** Confirm state */
    sealed class ConfirmStakingState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
            val feeState: FeeState,
            val validatorState: ValidatorState,
            val notifications: ImmutableList<StakingNotification>,
            val footerText: String,
            val isSuccess: Boolean,
            val isStaking: Boolean,
        ) : ConfirmStakingState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : ConfirmStakingState()
    }
}

enum class StakingStep {
    InitialInfo,
    Amount,
    Confirm,
    Validators,
    Success,
}