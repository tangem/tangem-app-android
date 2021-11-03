package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.onboarding.OnboardingManager
import com.tangem.tap.network.moonpay.MoonpayStatus
import com.tangem.tap.preferencesStorage
import org.rekotlin.Action

fun globalReducer(action: Action, state: AppState): GlobalState {

    if (action !is GlobalAction) return state.globalState

    val globalState = state.globalState

    return when (action) {
        is GlobalAction.SetResources -> {
            globalState.copy(resources = action.resources)
        }
        is GlobalAction.Onboarding.Start -> {
            val usedCardsPrefStorage = preferencesStorage.usedCardsPrefStorage
            val onboardingState = OnboardingManager(action.scanResponse, usedCardsPrefStorage)
            globalState.copy(onboardingManager = onboardingState)
        }
        is GlobalAction.Onboarding.Stop -> {
            globalState.copy(onboardingManager = null)
        }
        is GlobalAction.ScanFailsCounter.Increment -> {
            globalState.copy(scanCardFailsCounter = globalState.scanCardFailsCounter + 1)
        }
        is GlobalAction.ScanFailsCounter.Reset -> {
            globalState.copy(scanCardFailsCounter = 0)
        }
        is GlobalAction.SaveScanNoteResponse ->
            globalState.copy(scanResponse = action.scanResponse)
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
            val wallet = globalState.scanResponse?.card
                    ?.wallet(action.walletPublicKey)
                    ?.copy(
                        totalSignedHashes = action.walletSignedHashes,
                        remainingSignatures = action.remainingSignatures
                    )
            val card = globalState.scanResponse?.card
            wallet?.let { globalState.scanResponse.card.updateWallet(wallet) }

            if (card != null) {
                globalState.copy(scanResponse = globalState.scanResponse.copy(card = card))
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
        is GlobalAction.GetMoonPayStatus.Success -> {
            val fiatExchangeIsEnabled = globalState.configManager?.config?.isTopUpEnabled ?: false
            val moonpayStatus = if (fiatExchangeIsEnabled) {
                action.moonPayStatus
            } else {
                MoonpayStatus(
                    isBuyAllowed = false,
                    isSellAllowed = false,
                    availableToBuy = emptyList(),
                    availableToSell = emptyList()
                )
            }
            globalState.copy(moonpayStatus = moonpayStatus)
        }
        is GlobalAction.SetIfCardVerifiedOnline ->
            globalState.copy(cardVerifiedOnline = action.verified)

        else -> globalState
    }
}