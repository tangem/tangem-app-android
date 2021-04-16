package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.SecurityOption
import org.rekotlin.Action

fun globalReducer(action: Action, state: AppState): GlobalState {

    if (action !is GlobalAction) return state.globalState

    val globalState = state.globalState

    return when (action) {
        is GlobalAction.ScanFailsCounter.Increment -> {
            globalState.copy(scanCardFailsCounter = globalState.scanCardFailsCounter + 1)
        }
        is GlobalAction.ScanFailsCounter.Reset -> {
            globalState.copy(scanCardFailsCounter = 0)
        }
        is GlobalAction.SaveScanNoteResponse ->
            globalState.copy(scanNoteResponse = action.scanNoteResponse)
        is GlobalAction.ChangeAppCurrency -> {
            globalState.copy(appCurrency = action.appCurrency)
        }
        is GlobalAction.RestoreAppCurrency.Success -> {
            globalState.copy(appCurrency = action.appCurrency)
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