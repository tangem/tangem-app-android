package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.list.RoundedListWithDividersItemData
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.staking.model.stakekit.PendingAction
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
    val rewardsValidatorsState: StakingStates.RewardsValidatorsState,
    val confirmationState: StakingStates.ConfirmationState,
    val isBalanceHidden: Boolean,
    val bottomSheetConfig: TangemBottomSheetConfig?,
    val routeType: RouteType,
    val event: StateEvent<StakingEvent>,
) {

    fun copyWrapped(
        initialInfoState: StakingStates.InitialInfoState = this.initialInfoState,
        amountState: AmountState = this.amountState,
        confirmationState: StakingStates.ConfirmationState = this.confirmationState,
    ): StakingUiState = copy(
        initialInfoState = initialInfoState,
        amountState = amountState,
        confirmationState = confirmationState,
    )
}

internal sealed class StakingStates {

    abstract val isPrimaryButtonEnabled: Boolean

    /** Initial info state */
    sealed class InitialInfoState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
            val infoItems: ImmutableList<RoundedListWithDividersItemData>,
            val aprRange: TextReference,
            val onInfoClick: (InfoType) -> Unit,
            val yieldBalance: InnerYieldBalanceState,
            val isStakeMoreAvailable: Boolean,
        ) : InitialInfoState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : InitialInfoState()
    }

    /** Select validator to claim rewards state */
    sealed class RewardsValidatorsState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
            val rewards: ImmutableList<BalanceState>,
        ) : RewardsValidatorsState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : RewardsValidatorsState()
    }

    /** Confirmation state */
    sealed class ConfirmationState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
            val innerState: InnerConfirmationStakingState,
            val feeState: FeeState,
            val validatorState: ValidatorState,
            val pendingActions: ImmutableList<PendingAction>,
            val notifications: ImmutableList<StakingNotification>,
            val footerText: String,
            val transactionDoneState: TransactionDoneState,
            val pendingActionInProgress: PendingAction? = null,
        ) : ConfirmationState()

        data class Empty(
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : ConfirmationState()
    }
}

enum class StakingStep {
    InitialInfo,
    RewardsValidators,
    Amount,
    Confirmation,
    Validators,
}

enum class RouteType {
    STAKE,
    UNSTAKE,
    CLAIM,
    OTHER,
}
