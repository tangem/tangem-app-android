package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

fun globalReducer(action: Action, state: AppState): GlobalState {

    if (action !is GlobalAction) return state.globalState

    var newState = state.globalState

    when (action) {
        is GlobalAction.SaveScanNoteResponse ->
            newState = newState.copy(scanNoteResponse = action.scanNoteResponse)
        is GlobalAction.SetFiatRate -> {
            val rates = newState.fiatRates.rates.toMutableMap()
            rates[action.fiatRates.first] = action.fiatRates.second
            newState = newState.copy(fiatRates = FiatRates(rates))
        }
    }
    return newState
}