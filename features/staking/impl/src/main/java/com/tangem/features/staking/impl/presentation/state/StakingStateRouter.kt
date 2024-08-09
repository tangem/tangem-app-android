package com.tangem.features.staking.impl.presentation.state

import com.tangem.common.routing.AppRouter
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType

internal class StakingStateRouter(
    private val appRouter: AppRouter,
    private val stateController: StakingStateController,
) {

    fun onBackClick() {
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
            StakingStep.Confirmation -> {
                // TODO staking handle
            }
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
        stateController.update { it.copy(currentStep = StakingStep.InitialInfo) }
    }

    fun showRewardsValidators() {
        stateController.update { it.copy(currentStep = StakingStep.RewardsValidators) }
    }

    fun showAmount() {
        stateController.update { it.copy(currentStep = StakingStep.Amount) }
    }

    fun showValidators() {
        stateController.update { it.copy(currentStep = StakingStep.Validators) }
    }

    fun showConfirmation() {
        stateController.update { it.copy(currentStep = StakingStep.Confirmation) }
    }
}