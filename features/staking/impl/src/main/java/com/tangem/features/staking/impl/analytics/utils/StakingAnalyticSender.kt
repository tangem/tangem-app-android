package com.tangem.features.staking.impl.analytics.utils

import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.staking.impl.analytics.StakeScreenSource
import com.tangem.features.staking.impl.analytics.StakingAnalyticsEvents
import com.tangem.features.staking.impl.presentation.state.*

internal class StakingAnalyticSender(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun initialInfoScreen(value: StakingUiState) {
        val initialInfoState = value.initialInfoState as? StakingStates.InitialInfoState.Data
        val validatorState = initialInfoState?.yieldBalance as? InnerYieldBalanceState.Data
        val validatorCount = validatorState?.balance
            ?.filterNot { it.validator?.address.isNullOrBlank() }
            ?.distinctBy { it.validator?.address }
            ?.size ?: 0

        analyticsEventHandler.send(
            StakingAnalyticsEvents.StakingInfoScreenOpened(
                validatorsCount = validatorCount,
                token = value.cryptoCurrencySymbol,
            ),
        )
    }

    fun confirmationScreen(value: StakingUiState) {
        val confirmationState = value.confirmationState as? StakingStates.ConfirmationState.Data
        val validatorState = value.validatorState as? StakingStates.ValidatorState.Data
        val validatorName = validatorState?.chosenValidator?.name ?: return

        if (confirmationState?.innerState == InnerConfirmationStakingState.COMPLETED) return

        analyticsEventHandler.send(
            StakingAnalyticsEvents.ConfirmationScreenOpened(
                token = value.cryptoCurrencySymbol,
                validator = validatorName,
                action = getStakingActionType(value),
            ),
        )
    }

    fun screenCancel(value: StakingUiState) {
        analyticsEventHandler.send(
            StakingAnalyticsEvents.ButtonCancel(
                source = when (value.currentStep) {
                    StakingStep.InitialInfo -> StakeScreenSource.Info
                    StakingStep.Amount -> StakeScreenSource.Amount
                    StakingStep.Confirmation -> StakeScreenSource.Confirmation
                    StakingStep.Validators,
                    StakingStep.RestakeValidator,
                    StakingStep.RewardsValidators,
                    -> StakeScreenSource.Validators
                },
                token = value.cryptoCurrencyName,
            ),
        )
    }

    fun sendTransactionApprovalAnalytics(tokenCryptoCurrency: CryptoCurrency) {
        analyticsEventHandler.send(
            Basic.TransactionSent(
                sentFrom = AnalyticsParam.TxSentFrom.Approve(
                    blockchain = tokenCryptoCurrency.network.name,
                    token = tokenCryptoCurrency.symbol,
                    feeType = AnalyticsParam.FeeType.Normal,
                    permissionType = ApproveType.LIMITED.name,
                ),
                memoType = Basic.TransactionSent.MemoType.Null,
            ),
        )
    }

    fun sendTransactionStakingAnalytics(value: StakingUiState) {
        val validatorState = value.validatorState as? StakingStates.ValidatorState.Data
        val validatorName = validatorState?.chosenValidator?.name ?: return

        analyticsEventHandler.send(
            Basic.TransactionSent(
                sentFrom = AnalyticsParam.TxSentFrom.Staking(
                    blockchain = value.cryptoCurrencyName,
                    token = value.cryptoCurrencySymbol,
                    feeType = AnalyticsParam.FeeType.Normal,
                ),
                memoType = Basic.TransactionSent.MemoType.Null,
            ),
        )
        analyticsEventHandler.send(
            StakingAnalyticsEvents.StakeInProgressScreenOpened(
                validator = validatorName,
                token = value.cryptoCurrencySymbol,
                action = getStakingActionType(value),
            ),
        )
    }

    fun sendTransactionStakingClickedAnalytics(value: StakingUiState) {
        val validatorState = value.validatorState as? StakingStates.ValidatorState.Data
        val validatorName = validatorState?.chosenValidator?.name ?: return

        analyticsEventHandler.send(
            StakingAnalyticsEvents.ButtonAction(
                action = getStakingActionType(value),
                token = value.cryptoCurrencySymbol,
                validator = validatorName,
            ),
        )
    }

    private fun getStakingActionType(value: StakingUiState): StakingActionType {
        val confirmationState = value.confirmationState as? StakingStates.ConfirmationState.Data

        return when (value.actionType) {
            StakingActionCommonType.Enter -> StakingActionType.STAKE
            StakingActionCommonType.Exit -> StakingActionType.UNSTAKE
            is StakingActionCommonType.Pending -> confirmationState?.pendingAction?.type ?: StakingActionType.UNKNOWN
        }
    }
}
