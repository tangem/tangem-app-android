package com.tangem.tap.features.saveWallet.redux

import com.tangem.common.CompletionResult
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

internal class SaveWalletMiddleware {
    val middleware: Middleware<AppState> = { _, stateProvider ->
        { next ->
            { action ->
                val state = stateProvider()
                if (action is SaveWalletAction && state != null) {
                    handleAction(action, state.saveWalletState)
                }
                next(action)
            }
        }
    }

    private fun handleAction(action: SaveWalletAction, state: SaveWalletState) {
        when (action) {
            is SaveWalletAction.Save -> saveWalletIfBiometricsEnrolled(state)
            is SaveWalletAction.EnrollBiometrics.Enroll -> enrollBiometrics()
            is SaveWalletAction.SaveWalletWasShown -> saveWalletWasShown()
            is SaveWalletAction.Dismiss -> dismiss(state)
            is SaveWalletAction.Save.Success,
            is SaveWalletAction.ProvideBackupInfo,
            is SaveWalletAction.CloseError,
            is SaveWalletAction.Save.Error,
            is SaveWalletAction.EnrollBiometrics,
            is SaveWalletAction.EnrollBiometrics.Cancel,
            -> Unit
        }
    }

    private fun enrollBiometrics() {
        store.dispatchOnMain(NavigationAction.OpenBiometricsSettings)
    }

    private fun saveWalletIfBiometricsEnrolled(state: SaveWalletState) {
        if (tangemSdkManager.needEnrollBiometrics) {
            store.dispatchOnMain(SaveWalletAction.EnrollBiometrics)
        } else {
            saveWallet(state)
        }
    }

    /**
     * Save user's wallet created from [com.tangem.tap.common.redux.global.GlobalState.scanResponse]
     * or from [SaveWalletState.backupInfo] if provided from
     * [com.tangem.tap.features.onboarding.OnboardingHelper.trySaveWalletAndNavigateToWalletScreen]
     *
     * If saved user's wallet was selected then pop back to [AppScreen.Wallet]
     * or navigate to [AppScreen.WalletSelector] otherwise
     *
     * TODO: Update that logic after onboarding and backup features refactoring
     * */
    private fun saveWallet(state: SaveWalletState) {
        val scanResponse = state.backupInfo?.scanResponse
            ?: store.state.globalState.scanResponse
            ?: return

        if (state.backupInfo != null) {
            // TODO: Remove after onboarding refactoring
            Analytics.send(Onboarding.EnableBiometrics(AnalyticsParam.OnOffState.On))
        } else {
            Analytics.send(MainScreen.EnableBiometrics(AnalyticsParam.OnOffState.On))
        }

        scope.launch {
            val userWallet = UserWalletBuilder(scanResponse)
                .backupCardsIds(state.backupInfo?.backupCardsIds)
                .build()

            val isFirstSavedWallet = !userWalletsListManager.hasSavedUserWallets

            saveAccessCodeIfNeeded(state.backupInfo?.accessCode, userWallet.cardsInWallet)
                .flatMap { userWalletsListManager.save(userWallet, canOverride = true) }
                .flatMap { userWalletsListManager.selectWallet(userWallet.walletId) }
                .doOnFailure { error ->
                    store.dispatchOnMain(SaveWalletAction.Save.Error(error))
                }
                .doOnSuccess {
                    preferencesStorage.shouldSaveUserWallets = true

                    // Enable saving access codes only if this is the first time user save the wallet
                    preferencesStorage.shouldSaveAccessCodes = isFirstSavedWallet ||
                        preferencesStorage.shouldSaveAccessCodes


                    tangemSdkManager.setAccessCodeRequestPolicy(
                        useBiometricsForAccessCode = preferencesStorage.shouldSaveAccessCodes &&
                            userWallet.hasAccessCode,
                    )

                    store.dispatchOnMain(SaveWalletAction.Save.Success)

                    store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                    store.onUserWalletSelected(userWallet)
                }
        }
    }

    private fun dismiss(state: SaveWalletState) {
        if (state.backupInfo != null) {
            // TODO: Remove after onboarding refactoring
            Analytics.send(Onboarding.EnableBiometrics(AnalyticsParam.OnOffState.Off))
        } else {
            Analytics.send(MainScreen.EnableBiometrics(AnalyticsParam.OnOffState.Off))
        }
    }

    private fun saveWalletWasShown() {
        preferencesStorage.shouldShowSaveUserWalletScreen = false
    }

    private suspend fun saveAccessCodeIfNeeded(
        accessCode: String?,
        cardsInWallet: Set<String>,
    ): CompletionResult<Unit> {
        return when {
            accessCode != null -> {
                tangemSdkManager.saveAccessCode(
                    accessCode = accessCode,
                    cardsIds = cardsInWallet,
                )
            }
            else -> {
                CompletionResult.Success(Unit)
            }
        }
    }
}
