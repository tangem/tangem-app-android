package com.tangem.features.send.impl.presentation.state

import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class StateRouter(
    private val appRouter: AppRouter,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val isEditingDisabled: Boolean,
) {
    private val mutableCurrentState: MutableStateFlow<SendUiCurrentScreen> = MutableStateFlow(getInitState())

    val currentState: StateFlow<SendUiCurrentScreen>
        get() = mutableCurrentState

    val isEditState: Boolean
        get() = currentState.value.isFromConfirmation

    fun clear() {
        mutableCurrentState.update { getInitState() }
    }

    fun popBackStack() {
        appRouter.pop()
    }

    fun onBackClick(isSuccess: Boolean = false) {
        val type = currentState.value.type
        when {
            isSuccess -> popBackStack()
            isEditingDisabled -> when (type) {
                SendUiStateType.EditFee -> showSend()
                else -> popBackStack()
            }
            else -> when (type) {
                SendUiStateType.Amount -> showRecipient()
                SendUiStateType.Fee -> showSend()
                SendUiStateType.Send -> showAmount()
                SendUiStateType.Recipient -> popBackStack()
                SendUiStateType.EditAmount -> showSend()
                SendUiStateType.EditRecipient -> showSend()
                SendUiStateType.EditFee -> showSend()
                else -> popBackStack()
            }
        }
    }

    fun onNextClick() {
        when (currentState.value.type) {
            SendUiStateType.Recipient -> showAmount()
            SendUiStateType.Amount,
            SendUiStateType.Fee,
            SendUiStateType.EditAmount,
            SendUiStateType.EditRecipient,
            SendUiStateType.EditFee,
            -> showSend()
            SendUiStateType.Send -> onBackClick()

            else -> popBackStack()
        }
    }

    fun onPrevClick() {
        if (isEditingDisabled) {
            popBackStack()
        } else {
            when (currentState.value.type) {
                SendUiStateType.Amount -> showRecipient()
                else -> popBackStack()
            }
        }
    }

    fun showAmount(isFromConfirmation: Boolean = false) {
        analyticsEventsHandler.send(SendAnalyticEvents.AmountScreenOpened)
        mutableCurrentState.update {
            if (isFromConfirmation) {
                SendUiCurrentScreen(SendUiStateType.EditAmount, true)
            } else {
                SendUiCurrentScreen(SendUiStateType.Amount, false)
            }
        }
    }

    fun showRecipient(isFromConfirmation: Boolean = false) {
        analyticsEventsHandler.send(SendAnalyticEvents.AddressScreenOpened)
        mutableCurrentState.update {
            if (isFromConfirmation) {
                SendUiCurrentScreen(SendUiStateType.EditRecipient, true)
            } else {
                SendUiCurrentScreen(SendUiStateType.Recipient, false)
            }
        }
    }

    fun showFee(isFromConfirmation: Boolean = false) {
        analyticsEventsHandler.send(SendAnalyticEvents.FeeScreenOpened)
        mutableCurrentState.update {
            if (isFromConfirmation) {
                SendUiCurrentScreen(SendUiStateType.EditFee, true)
            } else {
                SendUiCurrentScreen(SendUiStateType.Fee, false)
            }
        }
    }

    fun showSend() {
        analyticsEventsHandler.send(SendAnalyticEvents.ConfirmationScreenOpened)
        mutableCurrentState.update { SendUiCurrentScreen(SendUiStateType.Send, isFromConfirmation = false) }
    }

    private fun getInitState() = if (isEditingDisabled) {
        SendUiCurrentScreen(
            type = SendUiStateType.None,
            isFromConfirmation = false,
        )
    } else {
        SendUiCurrentScreen(
            type = SendUiStateType.Recipient,
            isFromConfirmation = false,
        )
    }
}