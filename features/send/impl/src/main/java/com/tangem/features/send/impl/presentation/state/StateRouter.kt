package com.tangem.features.send.impl.presentation.state

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

internal class StateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
) {
    var currentState: MutableStateFlow<SendUiStateType> = MutableStateFlow(SendUiStateType.Recipient)
        private set

    fun popBackStack() {
        fragmentManager.get()?.popBackStack()
    }

    fun onBackClick() {
        when (currentState.value) {
            SendUiStateType.Recipient -> popBackStack()
            SendUiStateType.Amount -> showRecipient()
            SendUiStateType.Fee -> showAmount()
            SendUiStateType.Send -> showFee()
        }
    }

    fun onNextClick() {
        when (currentState.value) {
            SendUiStateType.Recipient -> showAmount()
            SendUiStateType.Amount -> showFee()
            SendUiStateType.Fee -> showSend()
            SendUiStateType.Send -> onBackClick()
        }
    }

    fun onPrevClick() {
        when (currentState.value) {
            SendUiStateType.Recipient -> popBackStack()
            SendUiStateType.Amount -> showRecipient()
            SendUiStateType.Fee -> showAmount()
            SendUiStateType.Send -> popBackStack()
        }
    }

    fun showAmount() {
        currentState.update { SendUiStateType.Amount }
    }

    fun showRecipient() {
        currentState.update { SendUiStateType.Recipient }
    }

    fun showFee() {
        currentState.update { SendUiStateType.Fee }
    }

    private fun showSend() {
        currentState.update { SendUiStateType.Send }
    }
}