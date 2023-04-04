package com.tangem.tap.features.sprinklr.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

internal object SprinklrReducer {
    fun reduce(action: Action, state: AppState): SprinklrState {
        return if (action is SprinklrAction) {
            internalReduce(action, state.sprinklrState)
        } else {
            state.sprinklrState
        }
    }

    private fun internalReduce(action: SprinklrAction, state: SprinklrState): SprinklrState {
        return when (action) {
            is SprinklrAction.Init -> state
            is SprinklrAction.UpdateUrl -> state.copy(url = action.url)
            is SprinklrAction.UpdateSprinklrDomains -> state.copy(sprinklrDomains = action.domains)
        }
    }
}
