package com.tangem.tap.common.redux.global

import com.tangem.domain.redux.domainStore
import com.tangem.domain.redux.global.DomainGlobalAction
import com.tangem.tap.common.extensions.replaceBy
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.onboarding.OnboardingManager
import com.tangem.tap.preferencesStorage
import com.tangem.tap.proxy.AppStateHolder
import org.rekotlin.Action

@Suppress("LongMethod", "ComplexMethod")
fun globalReducer(action: Action, state: AppState, appStateHolder: AppStateHolder): GlobalState {
    if (action !is GlobalAction) return state.globalState

    val globalState = state.globalState

    return when (action) {
        is GlobalAction.Onboarding.Start -> {
            val usedCardsPrefStorage = preferencesStorage.usedCardsPrefStorage
            val onboardingManager = OnboardingManager(action.scanResponse, usedCardsPrefStorage)
            globalState.copy(onboardingState = OnboardingState(true, onboardingManager))
        }
        is GlobalAction.Onboarding.StartForUnfinishedBackup -> {
            globalState.copy(onboardingState = OnboardingState(true, null))
        }
        is GlobalAction.Onboarding.Stop -> {
            globalState.copy(onboardingState = OnboardingState(false))
        }
        is GlobalAction.ScanFailsCounter.Increment -> {
            globalState.copy(scanCardFailsCounter = globalState.scanCardFailsCounter + 1)
        }
        is GlobalAction.ScanFailsCounter.Reset -> {
            globalState.copy(scanCardFailsCounter = 0)
        }
        is GlobalAction.SaveScanResponse -> {
            appStateHolder.scanResponse = action.scanResponse
            domainStore.dispatch(DomainGlobalAction.SaveScanNoteResponse(action.scanResponse))
            globalState.copy(scanResponse = action.scanResponse)
        }
        is GlobalAction.ChangeAppCurrency -> {
            appStateHolder.appFiatCurrency = action.appCurrency
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
            val card = globalState.scanResponse?.card ?: return globalState
            val wallet = card.wallets
                .firstOrNull { it.publicKey.contentEquals(action.walletPublicKey) }
                ?: return globalState

            val newCardInstance = card.copy(
                wallets = card.wallets.toMutableList().also { walletsMutable ->
                    walletsMutable.replaceBy(
                        item = wallet.copy(
                            totalSignedHashes = action.walletSignedHashes,
                            remainingSignatures = action.remainingSignatures,
                        ),
                    ) { it.index == wallet.index }
                },
            )
            globalState.copy(scanResponse = globalState.scanResponse.copy(card = newCardInstance))
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
        is GlobalAction.ExchangeManager.Init.Success -> {
            globalState.copy(exchangeManager = action.exchangeManager)
        }
        is GlobalAction.SetIfCardVerifiedOnline ->
            globalState.copy(cardVerifiedOnline = action.verified)
        is GlobalAction.FetchUserCountry.Success -> globalState.copy(
            userCountryCode = action.countryCode,
        )
        else -> globalState
    }
}
