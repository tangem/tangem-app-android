package com.tangem.features.send.impl.presentation.state

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

internal class StateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
) {
    var currentState: MutableStateFlow<SendUiStateType> = MutableStateFlow(SendUiStateType.Amount)
        private set

    private var isFromSend: Boolean = false

    fun popBackStack() {
        fragmentManager.get()?.popBackStack()
    }

    fun onBackClick() {
        if (isFromSend) {
            showSend()
        } else {
            when (currentState.value) {
                SendUiStateType.Amount -> popBackStack()
                SendUiStateType.Recipient -> showAmount()
                SendUiStateType.Fee -> showRecipient()
                SendUiStateType.Send -> showFee()
            }
        }
    }

    fun onNextClick() {
        when (currentState.value) {
            SendUiStateType.Amount -> showRecipient()
            SendUiStateType.Recipient -> showFee()
            SendUiStateType.Fee -> showSend()
            SendUiStateType.Send -> onBackClick()
        }
    }

    fun onPrevClick() {
        when (currentState.value) {
            SendUiStateType.Amount -> popBackStack()
            SendUiStateType.Recipient -> showAmount()
            SendUiStateType.Fee -> showRecipient()
            SendUiStateType.Send -> popBackStack()
        }
    }

    fun showAmount(isFromSend: Boolean = false) {
        this.isFromSend = isFromSend
        currentState.update { SendUiStateType.Amount }
    }

    fun showRecipient(isFromSend: Boolean = false) {
        this.isFromSend = isFromSend
        currentState.update { SendUiStateType.Recipient }
    }

    fun showFee(isFromSend: Boolean = false) {
        this.isFromSend = isFromSend
        currentState.update { SendUiStateType.Fee }
    }

    fun showSend() {
        currentState.update { SendUiStateType.Send }
    }
}