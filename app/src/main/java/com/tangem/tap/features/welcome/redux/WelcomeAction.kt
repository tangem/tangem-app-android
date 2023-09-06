package com.tangem.tap.features.welcome.redux

import android.content.Intent
import androidx.lifecycle.LifecycleCoroutineScope
import com.tangem.common.core.TangemError
import org.rekotlin.Action

internal sealed interface WelcomeAction : Action {
    object ProceedWithBiometrics : WelcomeAction {
        object Success : WelcomeAction
        data class Error(val error: TangemError) : WelcomeAction
    }

    data class ProceedWithCard(val lifecycleCoroutineScope: LifecycleCoroutineScope) : WelcomeAction {
        object Success : WelcomeAction
        data class Error(val error: TangemError) : WelcomeAction
    }

    data class SetInitialIntent(val intent: Intent?) : WelcomeAction

    object CloseError : WelcomeAction

    object ClearUserWallets : WelcomeAction
}