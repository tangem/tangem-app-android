package com.tangem.tap.features.welcome.redux

import android.content.Intent
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.domain.scanCard.ScanCardProcessor
import com.tangem.tap.intentHandler
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

internal class WelcomeMiddleware {
    val middleware: Middleware<AppState> = { _, appStateProvider ->
        { next ->
            { action ->
                val appState = appStateProvider()
                if (action is WelcomeAction && appState != null) {
                    handleAction(action, appState.welcomeState)
                }
                next(action)
            }
        }
    }

    private fun handleAction(action: WelcomeAction, state: WelcomeState) {
        when (action) {
            is WelcomeAction.ProceedWithBiometrics -> {
                proceedWithBiometry(state)
            }
            is WelcomeAction.ProceedWithCard -> {
                proceedWithCard(state)
            }
            is WelcomeAction.ProceedWithBiometrics.Error,
            is WelcomeAction.ProceedWithCard.Error,
            is WelcomeAction.ProceedWithBiometrics.Success,
            is WelcomeAction.ProceedWithCard.Success,
            is WelcomeAction.CloseError,
            is WelcomeAction.HandleDeepLink,
            -> Unit
        }
    }

    private fun proceedWithBiometry(state: WelcomeState) {
        scope.launch {
            userWalletsListManager.unlockWithBiometry()
                .doOnFailure { error ->
                    store.dispatchOnMain(WelcomeAction.ProceedWithBiometrics.Error(error))
                }
                .doOnSuccess { selectedUserWallet ->
                    if (selectedUserWallet != null) {
                        tangemSdkManager.setAccessCodeRequestPolicy(
                            useBiometricsForAccessCode = preferencesStorage.shouldSaveAccessCodes &&
                                selectedUserWallet.hasAccessCode,
                        )

                        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                        store.dispatchOnMain(WelcomeAction.ProceedWithBiometrics.Success)
                        store.onUserWalletSelected(selectedUserWallet)

                        handleDeepLinkIfNeeded(state.deepLinkIntent)
                    }
                }
        }
    }

    private fun proceedWithCard(state: WelcomeState) = scope.launch {
        scanCardInternal { scanResponse ->
            val userWallet = UserWalletBuilder(scanResponse).build()

            tangemSdkManager.setAccessCodeRequestPolicy(useBiometricsForAccessCode = false)
            userWalletsListManager.unlockWithCard(userWallet)
                .doOnFailure { error ->
                    store.dispatchOnMain(WelcomeAction.ProceedWithCard.Error(error))
                }
                .doOnSuccess {
                    store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                    store.dispatchOnMain(WelcomeAction.ProceedWithCard.Success)
                    store.onUserWalletSelected(userWallet)

                    handleDeepLinkIfNeeded(state.deepLinkIntent)
                }
        }
    }

    private fun handleDeepLinkIfNeeded(intent: Intent?) {
        intentHandler.handleWalletConnectLink(intent)
    }

    private suspend inline fun scanCardInternal(
        crossinline onCardScanned: suspend (ScanResponse) -> Unit,
    ) {
        ScanCardProcessor.scan(
            onSuccess = { scanResponse ->
                scope.launch { onCardScanned(scanResponse) }
            },
            onFailure = {
                store.dispatchOnMain(WelcomeAction.ProceedWithCard.Error(it))
            },
            onWalletNotCreated = {
                store.dispatchOnMain(WelcomeAction.ProceedWithCard.Success)
            },
        )
    }
}
