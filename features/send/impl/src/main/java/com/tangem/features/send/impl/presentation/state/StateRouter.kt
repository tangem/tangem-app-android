package com.tangem.features.send.impl.presentation.state

import androidx.fragment.app.FragmentManager
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.analytics.SendScreenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

internal class StateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val isEditingDisabled: Boolean,
) {
    private var mutableCurrentState: MutableStateFlow<SendUiCurrentScreen> = MutableStateFlow(getInitState())

    val currentState: StateFlow<SendUiCurrentScreen>
        get() = mutableCurrentState

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
            isEditingDisabled -> when (type) {
                SendUiStateType.Send -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Fee))
                    showFee()
                }
                else -> popBackStack()
            }
            else -> when (type) {
                SendUiStateType.Amount -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Address))
                    continueToSend(::showRecipient)
                }
                SendUiStateType.Fee -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Amount))
                    continueToSend(::showAmount)
                }
                SendUiStateType.Send -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Fee))
                    continueToSend(::showFee)
                }
                else -> continueToSend(::popBackStack)
            }
        }
    }

    fun onNextClick(): SendUiStateType {
        val prevState = currentState.value.type
        when (currentState.value.type) {
            SendUiStateType.Recipient -> {
                analyticsEventsHandler.send(SendAnalyticEvents.NextButtonClicked(SendScreenSource.Amount))
                continueToSend(::showAmount)
            }
            SendUiStateType.Amount -> {
                analyticsEventsHandler.send(SendAnalyticEvents.NextButtonClicked(SendScreenSource.Fee))
                continueToSend(::showFee)
            }
            SendUiStateType.Fee -> {
                analyticsEventsHandler.send(SendAnalyticEvents.NextButtonClicked(SendScreenSource.Fee))
                showSend()
            }
            SendUiStateType.Send -> {
                onBackClick()
            }
            else -> popBackStack()
        }
        return prevState
    }

    fun onPrevClick() {
        if (isEditingDisabled) {
            popBackStack()
        } else {
            when (currentState.value.type) {
                SendUiStateType.Amount -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Amount))
                    showRecipient()
                }
                SendUiStateType.Fee -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Fee))
                    showAmount()
                }
                else -> popBackStack()
            }
        }
    }

    fun showAmount(isFromConfirmation: Boolean = false) {
        analyticsEventsHandler.send(SendAnalyticEvents.AmountScreenOpened)
        mutableCurrentState.update { SendUiCurrentScreen(SendUiStateType.Amount, isFromConfirmation) }
    }

    fun showRecipient(isFromConfirmation: Boolean = false) {
        analyticsEventsHandler.send(SendAnalyticEvents.AddressScreenOpened)
        mutableCurrentState.update { SendUiCurrentScreen(SendUiStateType.Recipient, isFromConfirmation) }
    }

    fun showFee(isFromConfirmation: Boolean = false) {
        analyticsEventsHandler.send(SendAnalyticEvents.FeeScreenOpened)
        mutableCurrentState.update { SendUiCurrentScreen(SendUiStateType.Fee, isFromConfirmation) }
    }

    fun showSend() {
        analyticsEventsHandler.send(SendAnalyticEvents.ConfirmationScreenOpened)
        mutableCurrentState.update { SendUiCurrentScreen(SendUiStateType.Send, isFromConfirmation = false) }
    }

    private fun continueToSend(show: () -> Unit) {
        if (currentState.value.isFromConfirmation) showSend() else show()
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