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
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.domain.userWalletList.di.provideBiometricImplementation
import com.tangem.tap.domain.userWalletList.isLockable
import com.tangem.tap.features.wallet.redux.WalletAction
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
            val userWallet = userWalletsListManager.selectedUserWalletSync
                ?: UserWalletBuilder(scanResponse)
                    .backupCardsIds(state.backupInfo?.backupCardsIds)
                    .build()
                ?: return@launch

            provideLockableUserWalletsListManagerIfNot()

            val isFirstSavedWallet = !userWalletsListManager.hasUserWallets

            saveAccessCodeIfNeeded(state.backupInfo?.accessCode, userWallet.cardsInWallet)
                .flatMap { userWalletsListManager.save(userWallet, canOverride = true) }
                .doOnFailure { error ->
                    store.dispatchWithMain(SaveWalletAction.Save.Error(error))
                }
                .doOnSuccess {
                    preferencesStorage.shouldSaveUserWallets = true
                    // Enable saving access codes only if this is the first time user save the wallet
                    if (isFirstSavedWallet) {
                        preferencesStorage.shouldSaveAccessCodes = true
                        tangemSdkManager.setAccessCodeRequestPolicy(
                            useBiometricsForAccessCode = userWallet.hasAccessCode,
                        )
                    }

                    val savedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
                        Timber.e("User wallet is not saved")
                        return@launch
                    }
                    store.dispatchWithMain(SaveWalletAction.Save.Success)
                    store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                    store.dispatchWithMain(WalletAction.UpdateCanSaveUserWallets(canSaveUserWallets = true))
                    store.dispatchWithMain(
                        action = WalletAction.MultiWallet.CheckForBackupWarning(savedUserWallet.scanResponse.card),
                    )
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
        val manager = UserWalletsListManager.provideBiometricImplementation(
            context = context,
            tangemSdkManager = tangemSdkManager,
        )

        store.dispatchWithMain(GlobalAction.UpdateUserWalletsListManager(manager))
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