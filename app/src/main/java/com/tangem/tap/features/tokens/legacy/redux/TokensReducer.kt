package com.tangem.tap.features.tokens.legacy.redux

import com.tangem.domain.tokens.TokensAction
import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

object TokensReducer {
    fun reduce(action: Action, state: AppState): TokensState = internalReduce(action, state)
}

private fun internalReduce(action: Action, state: AppState): TokensState {
    if (action !is TokensAction) return state.tokensState

    return when (action) {
        is TokensAction.SetArgs.ManageAccess -> state.tokensState.copy(isManageAccess = true)
        is TokensAction.SetArgs.ReadAccess -> state.tokensState.copy(isManageAccess = false)
        else -> state.tokensState
    }
}