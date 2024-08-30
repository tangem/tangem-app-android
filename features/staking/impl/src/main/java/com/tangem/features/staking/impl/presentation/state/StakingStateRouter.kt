package com.tangem.features.staking.impl.presentation.state

import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.analytics.StakingAnalyticsEvents
import com.tangem.features.staking.impl.analytics.utils.StakingAnalyticSender

internal class StakingStateRouter(
    private val appRouter: AppRouter,
    private val stateController: StakingStateController,
    private val analyticsEventsHandler: AnalyticsEventHandler,
) {

    private val analyticSender = StakingAnalyticSender(analyticsEventsHandler)

    fun onBackClick() {
        analyticSender.screenCancel(stateController.value)
        appRouter.pop()
        stateController.clear()
    }

    fun onNextClick() {
        when (stateController.value.currentStep) {
            StakingStep.InitialInfo -> when (stateController.value.actionType) {
                StakingActionCommonType.ENTER -> showAmount()
                StakingActionCommonType.PENDING_OTHER,
                StakingActionCommonType.EXIT,
                -> showConfirmation()
                StakingActionCommonType.PENDING_REWARDS -> showRewardsValidators()
            }
            StakingStep.RewardsValidators,
            StakingStep.Validators,
            StakingStep.Amount,
            -> showConfirmation()
            StakingStep.Confirmation -> showInitial()
        }
    }

    fun onPrevClick() {
        val uiState = stateController.uiState.value
        when (uiState.currentStep) {
            StakingStep.InitialInfo -> onBackClick()
            StakingStep.Amount -> showInitial()
            StakingStep.Confirmation -> {
                if (uiState.actionType != StakingActionCommonType.ENTER) {
                    showInitial()
                } else {
                    showAmount()
                }
            }
            StakingStep.Validators -> showConfirmation()
            StakingStep.RewardsValidators -> showInitial()
        }
    }

    private fun showInitial() {
        analyticSender.initialInfoScreen(stateController.value)
        stateController.update { it.copy(currentStep = StakingStep.InitialInfo) }
    }

    private fun showRewardsValidators() {
        analyticsEventsHandler.send(
            StakingAnalyticsEvents.RewardScreenOpened(stateController.value.cryptoCurrencySymbol),
        )
        stateController.update { it.copy(currentStep = StakingStep.RewardsValidators) }
    }

    private fun showAmount() {
        analyticsEventsHandler.send(
            StakingAnalyticsEvents.AmountScreenOpened(stateController.value.cryptoCurrencySymbol),
        )
        stateController.update { it.copy(currentStep = StakingStep.Amount) }
    }

    fun showValidators() {
        stateController.update { it.copy(currentStep = StakingStep.Validators) }
    }

    private fun showConfirmation() {
        analyticSender.confirmationScreen(stateController.value)
        stateController.update { it.copy(currentStep = StakingStep.Confirmation) }
    }
}