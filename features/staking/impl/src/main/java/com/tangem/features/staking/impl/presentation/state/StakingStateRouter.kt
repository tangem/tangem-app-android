package com.tangem.features.staking.impl.presentation.state

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.update
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
                StakingStep.ValidatorAndFee -> showAmount()
                else -> popBackStack()
            }
        }
    }

    fun onNextClick() {
        when (stateController.uiState.value.currentStep) {
            StakingStep.InitialInfo -> showAmount()
            StakingStep.Amount -> showValidator()
            StakingStep.ValidatorAndFee -> showConfirm()
            StakingStep.Confirm -> onBackClick()
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

    fun showValidator() {
        stateController.update { it.copy(currentStep = StakingStep.ValidatorAndFee) }
    }

    fun showConfirm() {
        stateController.update { it.copy(currentStep = StakingStep.Confirm) }
    }

    private fun getInitialState() = StakingStep.InitialInfo
}