package com.tangem.tap.features.welcome.redux

import com.tangem.common.core.TangemError
import org.rekotlin.Action

internal sealed interface WelcomeAction : Action {

    data object ProceedWithBiometrics : WelcomeAction {
        object Success : WelcomeAction
        data class Error(val error: TangemError) : WelcomeAction
    }

    object ProceedWithCard : WelcomeAction {
        object Success : WelcomeAction
        data class Error(val error: TangemError) : WelcomeAction
        data class ChangeProgress(val isProgress: Boolean) : WelcomeAction
    }

    object CloseError : WelcomeAction

    object ClearUserWallets : WelcomeAction
}