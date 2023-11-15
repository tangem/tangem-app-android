package com.tangem.features.send.impl.presentation.state

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference

internal class StateRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
) {
    var currentState: MutableStateFlow<SendUiStateType> = MutableStateFlow(SendUiStateType.Amount)

    fun onBackClick() {
        fragmentManager.get()?.popBackStack()
    }

    fun onNextClick() {
        when (currentState.value) {
            SendUiStateType.Amount -> currentState.update { SendUiStateType.Recipient }
            SendUiStateType.Recipient -> currentState.update { SendUiStateType.Fee }
            SendUiStateType.Fee -> currentState.update { SendUiStateType.Send }
            SendUiStateType.Send -> currentState.update { SendUiStateType.Done }
            SendUiStateType.Done -> onBackClick()
        }
    }

    fun onPrevClick() {
        when (currentState.value) {
            SendUiStateType.Amount -> onBackClick()
            SendUiStateType.Recipient -> currentState.update { SendUiStateType.Amount }
            SendUiStateType.Fee -> currentState.update { SendUiStateType.Recipient }
            SendUiStateType.Send -> currentState.update { SendUiStateType.Fee }
            SendUiStateType.Done -> onBackClick()
        }
    }
}
