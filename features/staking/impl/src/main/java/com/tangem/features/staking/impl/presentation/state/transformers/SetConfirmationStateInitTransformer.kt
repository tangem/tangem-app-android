package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.utils.isCompositePendingActions
import com.tangem.features.staking.impl.presentation.state.utils.isTronStakedBalance
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SetConfirmationStateInitTransformer(
    private val isEnter: Boolean,
    private val isExplicitExit: Boolean,
    private val balanceState: BalanceState?,
    private val stakingApproval: StakingApproval,
    private val stakingAllowance: BigDecimal,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val yieldArgs: Yield.Args,
    private val pendingActions: ImmutableList<PendingAction>? = null,
    private val pendingAction: PendingAction? = pendingActions?.firstOrNull(),
) : Transformer<StakingUiState> {

    private val networkId
        get() = cryptoCurrencyStatus.currency.network.rawId

    private val isComposePendingActions
        get() = isCompositePendingActions(networkId, pendingActions)

    private val isTronStakedBalance
        get() = isTronStakedBalance(networkId, pendingAction)

    private val isImplicitExit: Boolean
        get() = pendingAction == null && pendingActions?.isEmpty() == true || isTronStakedBalance

    @Suppress("CyclomaticComplexMethod")
    override fun transform(prevState: StakingUiState): StakingUiState {
        val actionType = getActionType(prevState)
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
                isAmountEditable = actionType is StakingActionCommonType.Enter && !actionType.skipEnterAmount ||
                    actionType is StakingActionCommonType.Exit && !actionType.partiallyUnstakeDisabled,
                reduceAmountBy = null,
                pendingAction = pendingAction,
                pendingActions = pendingActions.takeIf { isComposePendingActions },
            ),
        )
    }

    private fun getActionType(prevState: StakingUiState): StakingActionCommonType {
        val isPartialEnterAmountDisabled = yieldArgs.enter.isPartialAmountDisabled
        val isPartialExitAmountDisabled = isPartiallyUnstakeDisabled(prevState)
        return when {
            isEnter -> StakingActionCommonType.Enter(isPartialEnterAmountDisabled)
            isImplicitExit || isExplicitExit -> StakingActionCommonType.Exit(isPartialExitAmountDisabled)
            else -> when (pendingAction?.type) {
                StakingActionType.STAKE -> StakingActionCommonType.Pending.Stake(isPartialEnterAmountDisabled)
                StakingActionType.UNSTAKE -> StakingActionCommonType.Exit(isPartialExitAmountDisabled)
                StakingActionType.CLAIM_REWARDS,
                StakingActionType.RESTAKE_REWARDS,
                -> StakingActionCommonType.Pending.Rewards
                StakingActionType.VOTE_LOCKED,
                StakingActionType.RESTAKE,
                -> StakingActionCommonType.Pending.Restake
                else -> StakingActionCommonType.Pending.Other
            }
        }
    }

    private fun isPartiallyUnstakeDisabled(state: StakingUiState): Boolean {
        val isSolana = BlockchainUtils.isSolana(state.cryptoCurrencyBlockchainId)
        val isValidatorPreferred = balanceState?.validator?.preferred == true

        return if (isSolana && !isValidatorPreferred) {
            true
        } else {
            yieldArgs.exit?.isPartialAmountDisabled == true
        }
    }
}