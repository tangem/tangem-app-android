package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.SecurityOption
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
        is GlobalAction.SetConfigManager -> {
            globalState.copy(configManager = action.configManager)
        }
        is GlobalAction.SetWarningManager -> globalState.copy(warningManager = action.warningManager)
        is GlobalAction.HideWarningMessage -> globalState
        is GlobalAction.UpdateSecurityOptions -> {
            val card = when (action.securityOption) {
                SecurityOption.LongTap -> globalState.scanNoteResponse?.card?.copy(
                        isPin1Default = true, isPin2Default = true
                )
                SecurityOption.PassCode -> globalState.scanNoteResponse?.card?.copy(
                        isPin1Default = true, isPin2Default = false
                )
                SecurityOption.AccessCode -> globalState.scanNoteResponse?.card?.copy(
                        isPin1Default = false, isPin2Default = true
                )
            }
            if (card != null) {
                globalState.copy(scanNoteResponse = globalState.scanNoteResponse?.copy(card = card))
            } else {
                globalState
            }
        }
        is GlobalAction.SetFeedbackManager -> {
            globalState.copy(feedbackManager = action.feedbackManager)
        }
        else -> globalState
    }
}