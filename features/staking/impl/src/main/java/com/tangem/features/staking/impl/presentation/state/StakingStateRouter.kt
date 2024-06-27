package com.tangem.features.staking.impl.presentation.state

import androidx.fragment.app.FragmentManager
import java.lang.ref.WeakReference

internal class StakingStateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
    private val stateController: StakingStateController,
) {

    private fun closeStaking() {
        fragmentManager.get()?.popBackStack()
        stateController.clear()
    }

    fun onBackClick(isSuccess: Boolean = false) {
        val type = stateController.uiState.value.currentStep
        when {
            isSuccess -> closeStaking()
            else -> when (type) {
                StakingStep.Amount -> showInitial()
                StakingStep.Confirm -> showAmount()
                else -> closeStaking()
            }
        }
    }

    fun onNextClick() {
        when (stateController.uiState.value.currentStep) {
            StakingStep.InitialInfo -> showAmount()
            StakingStep.Validators,
            StakingStep.Amount,
            -> showConfirm()
            StakingStep.Confirm -> showSuccess()
            StakingStep.Success -> closeStaking()
        }
    }

    fun onPrevClick() {
        when (stateController.uiState.value.currentStep) {
            StakingStep.Amount -> showInitial()
            StakingStep.Confirm -> showAmount()
            StakingStep.Success -> closeStaking()
            StakingStep.Validators -> showConfirm()
            else -> closeStaking()
        }
    }

    private fun showInitial() {
        stateController.update { it.copy(currentStep = StakingStep.InitialInfo) }
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

    fun showSuccess() {
        stateController.update { it.copy(currentStep = StakingStep.Success) }
    }
}