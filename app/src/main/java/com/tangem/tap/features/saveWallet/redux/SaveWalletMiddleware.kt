package com.tangem.tap.features.saveWallet.redux

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.common.flatMap
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.isLockable
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.userWalletList.di.provideBiometricImplementation
import com.tangem.tap.proxy.redux.DaggerGraphState
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber

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
            is SaveWalletAction.AllowToUseBiometrics -> allowToUseBiometrics(state)
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
* [REDACTED_TODO_COMMENT]
     * */
    private fun saveWallet(state: SaveWalletState) {
        val scanResponse = state.backupInfo?.scanResponse
            ?: store.state.globalState.scanResponse
            ?: return

        if (state.backupInfo != null) {
// [REDACTED_TODO_COMMENT]
            Analytics.send(Onboarding.EnableBiometrics(AnalyticsParam.OnOffState.On))
        } else {
            Analytics.send(MainScreen.EnableBiometrics(AnalyticsParam.OnOffState.On))
        }

        scope.launch {
            val userWallet = userWalletsListManager.selectedUserWalletSync
                ?: UserWalletBuilder(scanResponse)
                    .backupCardsIds(state.backupInfo?.backupCardsIds)
                    .build()
                ?: return@launch

            val featureToggles = store.inject(DaggerGraphState::userWalletsListManagerFeatureToggles)
            if (featureToggles.isGeneralManagerEnabled) {
                store.inject(DaggerGraphState::walletsRepository).saveShouldSaveUserWallets(item = true)
            } else {
                provideLockableUserWalletsListManagerIfNot()
            }

            val isFirstSavedWallet = !userWalletsListManager.hasUserWallets

            saveAccessCodeIfNeeded(accessCode = state.backupInfo?.accessCode, cardsInWallet = userWallet.cardsInWallet)
                .flatMap {
                    // Save wallet only at first time (SaveWalletBottomSheet).
                    // Otherwise (Example, add new wallet in Details) userWalletsListManager.wallets subscribers will
                    // receive useless updates.
                    // See: OnboardingHelper.trySaveWalletAndNavigateToWalletScreen()
                    if (isFirstSavedWallet) {
                        userWalletsListManager.save(userWallet, canOverride = true)
                    } else {
                        CompletionResult.Success(Unit)
                    }
                }
                .doOnFailure { error ->
                    store.dispatchWithMain(SaveWalletAction.Save.Error(error))
                }
                .doOnSuccess {
                    store.inject(DaggerGraphState::walletsRepository).saveShouldSaveUserWallets(item = true)

                    // Enable saving access codes only if this is the first time user save the wallet
                    if (isFirstSavedWallet) {
                        preferencesStorage.shouldSaveAccessCodes = true
                        store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
                            isBiometricsRequestPolicy = userWallet.hasAccessCode,
                        )
                    }

                    store.dispatchOnMain(SaveWalletAction.Save.Success)

                    store.dispatchOnMain(
                        if (store.state.navigationState.backStack.contains(AppScreen.Wallet)) {
                            NavigationAction.PopBackTo(AppScreen.Wallet)
                        } else {
                            NavigationAction.NavigateTo(AppScreen.Wallet)
                        },
                    )
                }
        }
    }

    private fun allowToUseBiometrics(state: SaveWalletState) {
        val scanResponse = state.backupInfo?.scanResponse
            ?: store.state.globalState.scanResponse
            ?: return

        if (state.backupInfo != null) {
// [REDACTED_TODO_COMMENT]
            Analytics.send(Onboarding.EnableBiometrics(AnalyticsParam.OnOffState.On))
        } else {
            Analytics.send(MainScreen.EnableBiometrics(AnalyticsParam.OnOffState.On))
        }

        scope.launch {
            val userWallet = userWalletsListManager.selectedUserWalletSync
                ?: UserWalletBuilder(scanResponse)
                    .backupCardsIds(state.backupInfo?.backupCardsIds)
                    .build()
                ?: return@launch

            store.inject(DaggerGraphState::walletsRepository).saveShouldSaveUserWallets(item = true)

            saveAccessCodeIfNeeded(accessCode = state.backupInfo?.accessCode, cardsInWallet = userWallet.cardsInWallet)
                .flatMap {
                    userWalletsListManager.save(userWallet, canOverride = true)
                }
                .doOnFailure { error ->
                    store.dispatchWithMain(SaveWalletAction.Save.Error(error))
                }
                .doOnSuccess {
                    preferencesStorage.shouldSaveAccessCodes = true
                    store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
                        isBiometricsRequestPolicy = userWallet.hasAccessCode,
                    )

                    store.dispatchOnMain(SaveWalletAction.Save.Success)
                    store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                }
        }
    }

    private suspend fun provideLockableUserWalletsListManagerIfNot() {
        if (store.state.globalState.userWalletsListManager?.isLockable == true) return

        val context = foregroundActivityObserver.foregroundActivity?.applicationContext.guard {
            val error = IllegalStateException("No activities in foreground")
            Timber.e(error)
            store.dispatchWithMain(SaveWalletAction.Save.Error(TangemSdkError.ExceptionError(error)))
            return
        }
        val manager = UserWalletsListManager.provideBiometricImplementation(context)

        store.dispatchWithMain(GlobalAction.UpdateUserWalletsListManager(manager))
    }

    private fun dismiss(state: SaveWalletState) {
        if (state.backupInfo != null) {
// [REDACTED_TODO_COMMENT]
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
