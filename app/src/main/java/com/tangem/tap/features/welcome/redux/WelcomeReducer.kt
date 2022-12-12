package com.tangem.tap.features.welcome.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

internal object WelcomeReducer {
    fun reduce(action: Action, state: AppState): WelcomeState {
        return if (action is WelcomeAction) {
            internalReduce(action, state.welcomeState)
        } else state.welcomeState
    }

    private fun internalReduce(action: WelcomeAction, state: WelcomeState): WelcomeState {
        return when (action) {
            is WelcomeAction.HandleDeepLink -> state.copy(deepLinkIntent = action.intent)
            is WelcomeAction.ProceedWithBiometrics -> state.copy(isUnlockWithBiometricsInProgress = true)
            is WelcomeAction.ProceedWithCard -> state.copy(isUnlockWithCardInProgress = true)
            is WelcomeAction.ProceedWithBiometrics.Error -> state.copy(
                error = action.error,
                isUnlockWithBiometricsInProgress = false,
            )
            is WelcomeAction.ProceedWithCard.Error -> state.copy(
                error = action.error,
                isUnlockWithCardInProgress = false,
            )
            is WelcomeAction.ProceedWithBiometrics.Success -> state.copy(isUnlockWithBiometricsInProgress = false)
            is WelcomeAction.ProceedWithCard.Success -> state.copy(isUnlockWithCardInProgress = false)
            is WelcomeAction.CloseError -> state.copy(error = null)
        }
    }
}
