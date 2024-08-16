package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class SetConfirmationStateLoadingTransformer(
    private val yield: Yield,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val possibleConfirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        val possibleValidatorState = possibleConfirmationState?.validatorState as? ValidatorState.Content
        val chosenValidator = possibleValidatorState?.chosenValidator ?: yield.validators[0]

        return prevState.copy(
            confirmationState = StakingStates.ConfirmationState.Data(
                isPrimaryButtonEnabled = false,
                innerState = InnerConfirmationStakingState.ASSENT,
                feeState = FeeState.Loading,
                validatorState = ValidatorState.Content(
                    isClickable = true,
                    chosenValidator = chosenValidator,
                    availableValidators = yield.validators,
                ),
                notifications = getNotifications(prevState),
                footerText = "",
                transactionDoneState = TransactionDoneState.Empty,
                pendingActions = persistentListOf(),
                pendingActionInProgress = null,
                isApprovalNeeded = false,
            ),
        )
    }

    private fun getNotifications(prevState: StakingUiState): ImmutableList<StakingNotification> {
        return persistentListOf(
            if (prevState.actionType == StakingActionCommonType.EXIT) {
                StakingNotification.Warning.Unstake(
                    cooldownPeriodDays = yield.metadata.cooldownPeriod.days,
                )
            } else {
                StakingNotification.Warning.EarnRewards(
                    currencyName = yield.token.name,
                    subtitleResourceId = getEarnRewardsPeriod(yield.metadata.rewardSchedule),
                )
            },
        )
    }

    private fun getEarnRewardsPeriod(rewardSchedule: Yield.Metadata.RewardSchedule): Int {
        return when (rewardSchedule) {
            Yield.Metadata.RewardSchedule.BLOCK,
            Yield.Metadata.RewardSchedule.DAY,
            Yield.Metadata.RewardSchedule.ERA,
            Yield.Metadata.RewardSchedule.EPOCH,
            -> R.string.staking_notification_earn_rewards_text_period_day

            Yield.Metadata.RewardSchedule.HOUR,
            -> R.string.staking_notification_earn_rewards_text_period_hour

            Yield.Metadata.RewardSchedule.WEEK,
            -> R.string.staking_notification_earn_rewards_text_period_week

            Yield.Metadata.RewardSchedule.MONTH,
            -> R.string.staking_notification_earn_rewards_text_period_month

            else
            -> R.string.staking_notification_earn_rewards_text_period_day
        }
    }
}