package com.tangem.features.staking.impl.presentation.state

import com.tangem.common.routing.AppRouter

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
            StakingStep.InitialInfo -> when (stateController.value.routeType) {
                RouteType.STAKE -> showAmount()
                RouteType.OTHER,
                RouteType.UNSTAKE,
                -> showConfirmation()
                RouteType.CLAIM -> showRewardsValidators()
            }
            StakingStep.RewardsValidators,
            StakingStep.Validators,
            StakingStep.Amount,
            -> showConfirmation()
            StakingStep.Confirmation -> {
// [REDACTED_TODO_COMMENT]
            }
        }
    }

    fun onPrevClick() {
        when (stateController.uiState.value.currentStep) {
            StakingStep.InitialInfo -> onBackClick()
            StakingStep.Amount -> showInitial()
            StakingStep.Confirmation -> showAmount()
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
