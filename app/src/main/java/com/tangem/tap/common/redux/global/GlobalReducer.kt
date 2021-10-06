package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

fun globalReducer(action: Action, state: AppState): GlobalState {

    if (action !is GlobalAction) return state.globalState

    val globalState = state.globalState

    return when (action) {
        is GlobalAction.Onboarding.Activate -> {
            globalState.copy(onboardingService = action.onboardingService)
        }
        is GlobalAction.Onboarding.Deactivate -> {
            globalState.copy(onboardingService = null)
        }
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
        is GlobalAction.SetConfigManager -> {
            globalState.copy(configManager = action.configManager)
        }
        is GlobalAction.SetWarningManager -> globalState.copy(warningManager = action.warningManager)
        is GlobalAction.UpdateWalletSignedHashes -> {
            val wallet = globalState.scanNoteResponse?.card
                ?.wallet(action.walletPublicKey)
                ?.copy(
                    totalSignedHashes = action.walletSignedHashes,
                    remainingSignatures = action.remainingSignatures
                )
            val card = globalState.scanNoteResponse?.card
            wallet?.let { globalState.scanNoteResponse.card.updateWallet(wallet) }

            if (card != null) {
                globalState.copy(scanNoteResponse = globalState.scanNoteResponse.copy(card = card))
            } else {
                globalState
            }
        }
        is GlobalAction.SetFeedbackManager -> {
            globalState.copy(feedbackManager = action.feedbackManager)
        }
        is GlobalAction.ShowDialog -> {
            globalState.copy(dialog = action.stateDialog)
        }
        is GlobalAction.HideDialog -> {
            globalState.copy(dialog = null)
        }
        is GlobalAction.GetMoonPayUserStatus.Success -> {
            globalState.copy(moonPayUserStatus = action.moonPayUserStatus)
        }
        is GlobalAction.SetIfCardVerifiedOnline ->
            globalState.copy(cardVerifiedOnline = action.verified)

        else -> globalState
    }
}