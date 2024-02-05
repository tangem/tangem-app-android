package com.tangem.features.send.impl.presentation.state

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

internal class StateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
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
                SendUiStateType.Send -> showFee()
                else -> popBackStack()
            }
            else -> when (currentState.value) {
                SendUiStateType.Amount -> showRecipient()
                SendUiStateType.Fee -> showAmount()
                SendUiStateType.Send -> showFee()
                else -> popBackStack()
            }
        }
    }

    fun onNextClick() {
        when (currentState.value) {
            SendUiStateType.Recipient -> showAmount()
            SendUiStateType.Amount -> showFee()
            SendUiStateType.Fee -> showSend()
            SendUiStateType.Send -> onBackClick()
            else -> popBackStack()
        }
    }

    fun onPrevClick() {
        if (isEditingDisabled) {
            popBackStack()
        } else {
            when (currentState.value) {
                SendUiStateType.Amount -> showRecipient()
                SendUiStateType.Fee -> showAmount()
                else -> popBackStack()
            }
        }
    }

    fun showAmount() {
        mutableCurrentState.update { SendUiStateType.Amount }
    }

    fun showRecipient() {
        mutableCurrentState.update { SendUiStateType.Recipient }
    }

    fun showFee() {
        mutableCurrentState.update { SendUiStateType.Fee }
    }

    private fun showSend() {
        mutableCurrentState.update { SendUiStateType.Send }
    }
}