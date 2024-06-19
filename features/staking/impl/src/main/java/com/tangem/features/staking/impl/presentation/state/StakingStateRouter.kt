package com.tangem.features.staking.impl.presentation.state

import androidx.fragment.app.FragmentManager
import java.lang.ref.WeakReference

internal class StakingStateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
    private val stateController: StakingStateController,
) {
    fun clear() {
        stateController.update { it.copy(currentStep = getInitialState()) }
    }

    fun popBackStack() {
        fragmentManager.get()?.popBackStack()
    }

    fun onBackClick(isSuccess: Boolean = false) {
        val type = stateController.uiState.value.currentStep
        when {
            isSuccess -> popBackStack()
            else -> when (type) {
                StakingStep.Amount -> showInitial()
                StakingStep.Confirm -> showAmount()
                else -> popBackStack()
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
            StakingStep.Success -> onBackClick()
        }
    }

    fun onPrevClick() {
        when (stateController.uiState.value.currentStep) {
            StakingStep.Amount -> showInitial()
            else -> popBackStack()
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

    private fun getInitialState() = StakingStep.InitialInfo
}