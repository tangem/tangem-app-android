package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

@Suppress("LongMethod", "ComplexMethod")
fun globalReducer(action: Action, state: AppState): GlobalState {
    if (action !is GlobalAction) return state.globalState

    val globalState = state.globalState

    return when (action) {
        is GlobalAction.SaveScanResponse -> {
            globalState.copy(scanResponse = action.scanResponse)
        }
        is GlobalAction.ChangeAppCurrency -> {
            globalState.copy(appCurrency = action.appCurrency)
        }
        is GlobalAction.RestoreAppCurrency.Success -> {
            globalState.copy(appCurrency = action.appCurrency)
        }
        is GlobalAction.IsSignWithRing -> globalState.copy(isLastSignWithRing = action.isSignWithRing)
        else -> globalState
    }
}