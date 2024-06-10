package com.tangem.features.staking.impl.presentation.state

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

internal class StakingStateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
) {
    private var mutableCurrentState: MutableStateFlow<StakingUiStateType> = MutableStateFlow(getInitState())

    val currentState: StateFlow<StakingUiStateType>
        get() = mutableCurrentState.asStateFlow()

    fun clear() {
        mutableCurrentState.update { getInitState() }
    }

    fun popBackStack() {
        fragmentManager.get()?.popBackStack()
    }

    fun onBackClick(isSuccess: Boolean = false) {
        val type = currentState.value
        when {
            isSuccess -> popBackStack()
            else -> when (type) {
                StakingUiStateType.Amount -> showInitial()
                StakingUiStateType.ValidatorAndFee -> showAmount()
                else -> popBackStack()
            }
        }
    }

    fun onNextClick() {
        when (currentState.value) {
            StakingUiStateType.InitialInfo -> showAmount()
            StakingUiStateType.Amount,
            StakingUiStateType.ValidatorAndFee,
            -> showConfirm()
            StakingUiStateType.Confirm -> onBackClick()
        }
    }

    fun onPrevClick() {
        when (currentState.value) {
            StakingUiStateType.Amount -> showInitial()
            else -> popBackStack()
        }
    }

    private fun showInitial() {
        mutableCurrentState.update { StakingUiStateType.InitialInfo }
    }

    fun showAmount() {
        mutableCurrentState.update { StakingUiStateType.Amount }
    }

    fun showValidator() {
        mutableCurrentState.update { StakingUiStateType.ValidatorAndFee }
    }

    fun showConfirm() {
        mutableCurrentState.update { StakingUiStateType.Confirm }
    }

    private fun getInitState() = StakingUiStateType.InitialInfo
}