package com.tangem.tap.features.signin.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

/**
 * @author Andrew Khokhlov on 21/03/2023
 */
object SignInReducer {
    fun reduce(action: Action, state: AppState): SignInState {
        if (action !is SignInAction) return state.signInState

        return when (action) {
            is SignInAction.SetSignInType -> state.signInState.copy(type = action.type)
        }
    }
}
