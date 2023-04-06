package com.tangem.tap.proxy.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

object DaggerGraphReducer {
    fun reduce(action: Action, state: AppState): DaggerGraphState {
        if (action !is DaggerGraphAction) return state.daggerGraphState

        return internalReduce(action, state)
    }

    private fun internalReduce(action: DaggerGraphAction, state: AppState): DaggerGraphState {
        return when (action) {
            is DaggerGraphAction.SetApplicationDependencies -> state.daggerGraphState.copy(
                assetReader = action.assetReader,
                networkConnectionManager = action.networkConnectionManager,
                tokensListFeatureToggles = action.tokensListFeatureToggles,
            )
            is DaggerGraphAction.SetActivityDependencies -> state.daggerGraphState.copy(
                testerRouter = action.testerRouter,
            )
        }
    }
}