package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.utils.isCompositePendingActions
import com.tangem.features.staking.impl.presentation.state.utils.isTronStakedBalance
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal class SetConfirmationStateInitTransformer(
    private val isEnter: Boolean,
    private val isExplicitExit: Boolean,
    private val balanceState: BalanceState?,
    private val stakingApproval: StakingApproval,
    private val stakingAllowance: BigDecimal,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val pendingActions: ImmutableList<PendingAction>? = null,
    private val pendingAction: PendingAction? = pendingActions?.firstOrNull(),

) : Transformer<StakingUiState> {

    private val networkId
        get() = cryptoCurrencyStatus.currency.network.id.value

    private val isComposePendingActions
        get() = isCompositePendingActions(networkId, pendingActions)

    private val isTronStakedBalance
        get() = isTronStakedBalance(networkId, pendingAction)

    private val isImplicitExit: Boolean
        get() = pendingAction == null && pendingActions?.isEmpty() == true || isTronStakedBalance

    override fun transform(prevState: StakingUiState): StakingUiState {
        val actionType = when {
            isEnter -> StakingActionCommonType.Enter
            isImplicitExit || isExplicitExit -> StakingActionCommonType.Exit
            else -> when (pendingAction?.type) {
                StakingActionType.STAKE -> StakingActionCommonType.Enter
                StakingActionType.UNSTAKE -> StakingActionCommonType.Exit
                StakingActionType.CLAIM_REWARDS,
                StakingActionType.RESTAKE_REWARDS,
                -> StakingActionCommonType.Pending.Rewards
                StakingActionType.VOTE_LOCKED,
                StakingActionType.RESTAKE,
                -> StakingActionCommonType.Pending.Restake
                else -> StakingActionCommonType.Pending.Other
            }
        }

        return prevState.copy(
            actionType = actionType,
            balanceState = balanceState,
            confirmationState = StakingStates.ConfirmationState.Data(
                isPrimaryButtonEnabled = false,
                innerState = InnerConfirmationStakingState.ASSENT,
                feeState = FeeState.Loading,
                notifications = persistentListOf(),
                footerText = TextReference.EMPTY,
                transactionDoneState = TransactionDoneState.Empty,
                isApprovalNeeded = stakingApproval is StakingApproval.Needed,
                allowance = stakingAllowance,
                reduceAmountBy = null,
                pendingAction = pendingAction,
                pendingActions = pendingActions.takeIf { isComposePendingActions },
            ),
        )
    }
}
