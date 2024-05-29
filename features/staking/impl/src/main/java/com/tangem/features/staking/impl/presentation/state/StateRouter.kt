package com.tangem.features.staking.impl.presentation.state

import androidx.fragment.app.FragmentManager
import com.tangem.core.analytics.api.AnalyticsEventHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

internal class StateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
    private val analyticsEventsHandler: AnalyticsEventHandler,
) {
    private var mutableCurrentState: MutableStateFlow<StakingUiCurrentScreen> = MutableStateFlow(getInitState())

    val currentState: StateFlow<StakingUiCurrentScreen>
        get() = mutableCurrentState

    val isEditState: Boolean
        get() = currentState.value.isFromConfirmation

    fun clear() {
        mutableCurrentState.update { getInitState() }
    }

    fun popBackStack() {
        fragmentManager.get()?.popBackStack()
    }

    fun onBackClick(isSuccess: Boolean = false) {
        val type = currentState.value.type
        when {
            isSuccess -> popBackStack()
            else -> when (type) {
                StakingUiStateType.Amount -> showInitial()
                StakingUiStateType.ValidatorAndFee -> showAmount()
                StakingUiStateType.EditAmount -> showConfirm()
                StakingUiStateType.EditValidator -> showConfirm()
                StakingUiStateType.EditFee -> showConfirm()
                else -> popBackStack()
            }
        }
    }

    fun onNextClick() {
        when (currentState.value.type) {
            StakingUiStateType.InitialInfo -> showAmount()
            StakingUiStateType.Amount,
            StakingUiStateType.ValidatorAndFee,
            StakingUiStateType.EditAmount,
            StakingUiStateType.EditValidator,
            StakingUiStateType.EditFee,
            -> showConfirm()
            StakingUiStateType.Confirm -> onBackClick()

            else -> popBackStack()
        }
    }

    fun onPrevClick() {
        when (currentState.value.type) {
            StakingUiStateType.Amount -> showInitial()
            else -> popBackStack()
        }
    }

    private fun showInitial() {
        mutableCurrentState.update {
            StakingUiCurrentScreen(StakingUiStateType.InitialInfo, isFromConfirmation = true)
        }
    }

    fun showAmount(isFromConfirmation: Boolean = false) {
        mutableCurrentState.update {
            if (isFromConfirmation) {
                StakingUiCurrentScreen(StakingUiStateType.EditAmount, true)
            } else {
                StakingUiCurrentScreen(StakingUiStateType.Amount, false)
            }
        }
    }

    fun showValidator(isFromConfirmation: Boolean = false) {
        mutableCurrentState.update {
            if (isFromConfirmation) {
                StakingUiCurrentScreen(StakingUiStateType.EditValidator, true)
            } else {
                StakingUiCurrentScreen(StakingUiStateType.ValidatorAndFee, false)
            }
        }
    }

    fun showConfirm() {
        mutableCurrentState.update { StakingUiCurrentScreen(StakingUiStateType.Confirm, isFromConfirmation = false) }
    }

    private fun getInitState() = StakingUiCurrentScreen(
        type = StakingUiStateType.InitialInfo,
        isFromConfirmation = false,
    )
}
