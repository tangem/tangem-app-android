package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

fun globalReducer(action: Action, state: AppState): GlobalState {

    if (action !is GlobalAction) return state.globalState

    val globalState = state.globalState

    return when (action) {
        is GlobalAction.SaveScanNoteResponse ->
            globalState.copy(scanNoteResponse = action.scanNoteResponse)
        is GlobalAction.SetFiatRate -> {
            val rates = globalState.conversionRates.rates.toMutableMap()
            rates[action.fiatRates.first] = action.fiatRates.second
            globalState.copy(conversionRates = ConversionRates(rates))
        }
        is GlobalAction.ChangeAppCurrency -> {
            globalState.copy(appCurrency = action.appCurrency, conversionRates = ConversionRates(mapOf()))
        }
        is GlobalAction.RestoreAppCurrency.Success -> {
            globalState.copy(appCurrency = action.appCurrency, conversionRates = ConversionRates(mapOf()))
        }
        is GlobalAction.UpdateWalletSignedHashes -> {
            val card = globalState.scanNoteResponse?.card?.copy(
                    walletSignedHashes = action.walletSignedHashes
            )
            if (card != null) {
                globalState.copy(scanNoteResponse = globalState.scanNoteResponse.copy(card = card))
            } else {
                globalState
            }
        }
        else -> globalState
    }
}