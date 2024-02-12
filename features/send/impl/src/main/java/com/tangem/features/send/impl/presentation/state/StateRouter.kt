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
    private var mutableCurrentState: MutableStateFlow<SendUiStateType> = MutableStateFlow(
        if (isEditingDisabled) {
            SendUiStateType.None
        } else {
            SendUiStateType.Recipient
        },
    )

    val currentState: StateFlow<SendUiStateType> = mutableCurrentState

    fun popBackStack() {
        fragmentManager.get()?.popBackStack()
    }

    fun onBackClick(isSuccess: Boolean = false) {
        when {
            isSuccess -> popBackStack()
            isEditingDisabled -> when (currentState.value) {
                SendUiStateType.Send -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Fee))
                    showFee()
                }
                else -> popBackStack()
            }
            else -> when (currentState.value) {
                SendUiStateType.Amount -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Address))
                    showRecipient()
                }
                SendUiStateType.Fee -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Amount))
                    showAmount()
                }
                SendUiStateType.Send -> {
                    analyticsEventsHandler.send(SendAnalyticEvents.BackButtonClicked(SendScreenSource.Fee))
                    showFee()
                }
                else -> popBackStack()
            }
        }
    }

    fun onNextClick(): SendUiStateType {
        val prevState = currentState.value
        when (currentState.value) {
            SendUiStateType.Recipient -> {
                analyticsEventsHandler.send(SendAnalyticEvents.NextButtonClicked(SendScreenSource.Amount))
                showAmount()
            }
            SendUiStateType.Amount -> {
                analyticsEventsHandler.send(SendAnalyticEvents.NextButtonClicked(SendScreenSource.Fee))
                showFee()
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
            when (currentState.value) {
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

    fun showAmount() {
        analyticsEventsHandler.send(SendAnalyticEvents.AmountScreenOpened)
        mutableCurrentState.update { SendUiStateType.Amount }
    }

    fun showRecipient() {
        analyticsEventsHandler.send(SendAnalyticEvents.AddressScreenOpened)
        mutableCurrentState.update { SendUiStateType.Recipient }
    }

    fun showFee() {
        analyticsEventsHandler.send(SendAnalyticEvents.FeeScreenOpened)
        mutableCurrentState.update { SendUiStateType.Fee }
    }

    private fun showSend() {
        analyticsEventsHandler.send(SendAnalyticEvents.ConfirmationScreenOpened)
        mutableCurrentState.update { SendUiStateType.Send }
    }
}
