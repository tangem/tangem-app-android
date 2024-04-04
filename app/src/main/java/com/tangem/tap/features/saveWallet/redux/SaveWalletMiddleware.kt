package com.tangem.tap.features.saveWallet.redux

import com.tangem.common.*
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.userWalletList.di.provideBiometricImplementation
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import org.rekotlin.Store
import timber.log.Timber

internal class SaveWalletMiddleware {

    private val saveWalletJobHolder = JobHolder()

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
            is SaveWalletAction.SaveWalletAfterBackup -> saveWalletAfterBackup(state)
            is SaveWalletAction.Save.Success,
            is SaveWalletAction.ProvideBackupInfo,
            is SaveWalletAction.CloseError,
            is SaveWalletAction.Save.Error,
            is SaveWalletAction.EnrollBiometrics,
            is SaveWalletAction.EnrollBiometrics.Cancel,
            -> Unit
        }
    }

    private fun saveWalletAfterBackup(state: SaveWalletState) {
        scope.launch {
            val backupInfo = state.backupInfo ?: error("Backup info is null")

            val userWallet = UserWalletBuilder(backupInfo.scanResponse)
                .backupCardsIds(state.backupInfo.backupCardsIds)
                .build()
                .guard {
                    Timber.e("User wallet not created")
                    return@launch
                }

            userWalletsListManager.save(userWallet, canOverride = true)
                .flatMap {
                    saveAccessCodeIfNeeded(accessCode = backupInfo.accessCode, cardsInWallet = userWallet.cardsInWallet)
                }
                .doOnFailure { error ->
                    Timber.e(error, "Unable to save user wallet")
                }
                .doOnSuccess { mainScope.launch { store.onUserWalletSelected(userWallet) } }
                .doOnResult { store.navigateToWallet() }
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
            if (!featureToggles.isGeneralManagerEnabled) {
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
                    store.navigateToWallet()
                }
        }.saveIn(saveWalletJobHolder)
    }

    private fun allowToUseBiometrics(state: SaveWalletState) {
        if (tangemSdkManager.needEnrollBiometrics) {
            store.dispatchOnMain(SaveWalletAction.EnrollBiometrics)
            return
        }

        if (state.backupInfo != null) {
// [REDACTED_TODO_COMMENT]
            Analytics.send(Onboarding.EnableBiometrics(AnalyticsParam.OnOffState.On))
        } else {
            Analytics.send(MainScreen.EnableBiometrics(AnalyticsParam.OnOffState.On))
        }

        scope.launch {
            /*
             * We don't need to save user wallet if it is not created from backup info,
             * because it will be automatically saved on UserWalletsListManager switch
             */
            val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
                val error = IllegalStateException("No selected user wallet")
                Timber.e(error, "Unable to save user wallet")
                store.dispatchWithMain(SaveWalletAction.Save.Error(TangemSdkError.ExceptionError(error)))
                return@launch
            }

            handleSuccessAllowing(selectedUserWallet)
        }.saveIn(saveWalletJobHolder)
    }

    private suspend fun handleSuccessAllowing(userWallet: UserWallet) {
        store.inject(DaggerGraphState::walletsRepository).saveShouldSaveUserWallets(item = true)
        preferencesStorage.shouldSaveAccessCodes = true
        store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
            isBiometricsRequestPolicy = userWallet.hasAccessCode,
        )

        store.dispatchWithMain(SaveWalletAction.Save.Success)
        store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Wallet))
    }

    private suspend fun provideLockableUserWalletsListManagerIfNot() {
        if (store.state.globalState.userWalletsListManager?.isLockable() == true) return

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
            accessCode.isNullOrBlank() -> {
                CompletionResult.Success(Unit)
            }
            else -> {
                tangemSdkManager.saveAccessCode(
                    accessCode = accessCode,
                    cardsIds = cardsInWallet,
                )
            }
        }
    }

    private suspend fun Store<AppState>.navigateToWallet() {
        dispatchWithMain(
            if (store.state.navigationState.backStack.contains(AppScreen.Wallet)) {
                NavigationAction.PopBackTo(AppScreen.Wallet)
            } else {
                NavigationAction.NavigateTo(AppScreen.Wallet)
            },
        )
    }
}
