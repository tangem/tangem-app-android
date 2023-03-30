package com.tangem.tap.features.signin.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
object SignInReducer {
    fun reduce(action: Action, state: AppState): SignInState {
        if (action !is SignInAction) return state.signInState

        return when (action) {
            is SignInAction.SetSignInType -> state.signInState.copy(type = action.type)
        }
    }
}