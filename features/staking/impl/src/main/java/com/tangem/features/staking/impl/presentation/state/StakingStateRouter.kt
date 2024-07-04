package com.tangem.features.staking.impl.presentation.state

import androidx.fragment.app.FragmentManager
import java.lang.ref.WeakReference

internal class StakingStateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
    private val stateController: StakingStateController,
) {

    fun onBackClick() {
        fragmentManager.get()?.popBackStack()
        stateController.clear()
    }

    fun onNextClick() {
        when (stateController.uiState.value.currentStep) {
            StakingStep.InitialInfo -> showAmount()
            StakingStep.RewardsValidators,
            StakingStep.Validators,
            StakingStep.Amount,
            -> showConfirm()
            StakingStep.Confirm -> {
                // TODO staking handle
            }
        }
    }

    fun onPrevClick() {
        when (stateController.uiState.value.currentStep) {
            StakingStep.InitialInfo -> onBackClick()
            StakingStep.Amount -> showInitial()
            StakingStep.Confirm -> showAmount()
            StakingStep.Validators -> showConfirm()
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

    fun showConfirm() {
        stateController.update { it.copy(currentStep = StakingStep.Confirm) }
    }
}
