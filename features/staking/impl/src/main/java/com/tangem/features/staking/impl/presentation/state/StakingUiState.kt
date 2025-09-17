package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.list.RoundedListWithDividersItemData
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.events.StakingEvent
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

/**
 * Ui states of the staking screen
 */
@Immutable
internal data class StakingUiState(
    val title: TextReference,
    val subtitle: TextReference?,
    val clickIntents: StakingClickIntents,
    val walletName: String,
    val cryptoCurrencyName: String,
    val cryptoCurrencySymbol: String,
    val cryptoCurrencyBlockchainId: String,
    val currentStep: StakingStep,
    val initialInfoState: StakingStates.InitialInfoState,
    val amountState: AmountState,
    val rewardsValidatorsState: StakingStates.RewardsValidatorsState,
    val confirmationState: StakingStates.ConfirmationState,
    val validatorState: StakingStates.ValidatorState,
    val isBalanceHidden: Boolean,
    val bottomSheetConfig: TangemBottomSheetConfig?,
    val actionType: StakingActionCommonType,
    val buttonsState: NavigationButtonsState,
    val event: StateEvent<StakingEvent>,
    val balanceState: BalanceState?,
    val showColdWalletInteractionIcon: Boolean,
) {

    fun copyWrapped(
        initialInfoState: StakingStates.InitialInfoState = this.initialInfoState,
        amountState: AmountState = this.amountState,
        confirmationState: StakingStates.ConfirmationState = this.confirmationState,
        validatorState: StakingStates.ValidatorState = this.validatorState,
    ): StakingUiState = copy(
        initialInfoState = initialInfoState,
        amountState = amountState,
        confirmationState = confirmationState,
        validatorState = validatorState,
    )
}

internal sealed class StakingStates {

    abstract val isPrimaryButtonEnabled: Boolean

    /** Initial info state */
    sealed class InitialInfoState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
            val showBanner: Boolean,
            val infoItems: ImmutableList<RoundedListWithDividersItemData>,
            val aprRange: TextReference,
            val onInfoClick: (InfoType) -> Unit,
            val yieldBalance: InnerYieldBalanceState,
            val pullToRefreshConfig: PullToRefreshConfig,
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

    sealed class ValidatorState : StakingStates() {
        abstract val isClickable: Boolean

        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
            override val isClickable: Boolean,
            val isVisibleOnConfirmation: Boolean,
            val chosenValidator: Yield.Validator,
            val activeValidator: Yield.Validator?,
            val availableValidators: List<Yield.Validator>,
        ) : ValidatorState()

        data class Empty(
            override val isClickable: Boolean = false,
            override val isPrimaryButtonEnabled: Boolean = false,
        ) : ValidatorState()
    }

    /** Confirmation state */
    sealed class ConfirmationState : StakingStates() {
        data class Data(
            override val isPrimaryButtonEnabled: Boolean,
            val innerState: InnerConfirmationStakingState,
            val feeState: FeeState,
            val pendingAction: PendingAction?,
            val pendingActions: ImmutableList<PendingAction>?,
            val notifications: ImmutableList<NotificationUM>,
            val footerText: TextReference,
            val transactionDoneState: TransactionDoneState,
            val isApprovalNeeded: Boolean,
            val isAmountEditable: Boolean,
            val allowance: BigDecimal,
            val reduceAmountBy: BigDecimal?,
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
    RestakeValidator,
    Confirmation,
    Validators,
}