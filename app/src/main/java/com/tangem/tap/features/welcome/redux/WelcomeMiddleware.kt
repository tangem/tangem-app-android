package com.tangem.tap.features.welcome.redux

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
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

internal class WelcomeMiddleware {
    val middleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                if (action is WelcomeAction) {
                    handleAction(action)
                }
                next(action)
            }
        }
    }

    private fun handleAction(action: WelcomeAction) {
        when (action) {
            is WelcomeAction.ProceedWithBiometrics -> proceedWithBiometry()
            is WelcomeAction.ProceedWithCard -> proceedWithCard()
            is WelcomeAction.ProceedWithBiometrics.Error -> Unit
            is WelcomeAction.ProceedWithCard.Error -> Unit
            is WelcomeAction.ProceedWithBiometrics.Success -> Unit
            is WelcomeAction.ProceedWithCard.Success -> Unit
            is WelcomeAction.CloseError -> Unit
        }
    }

    private fun proceedWithBiometry() {
        scope.launch {
            userWalletsListManager.unlockWithBiometry()
                .doOnFailure { error ->
                    store.dispatchOnMain(WelcomeAction.ProceedWithBiometrics.Error(error))
                }
                .doOnSuccess { selectedUserWallet ->
                    if (selectedUserWallet != null) {
                        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                        store.dispatchOnMain(WelcomeAction.ProceedWithBiometrics.Success)
                        store.onUserWalletSelected(selectedUserWallet)
                    }
                }
        }
    }

    private fun proceedWithCard() = scope.launch {
        scanCardInternal { scanResponse ->
            val userWallet = UserWalletBuilder(scanResponse).build()

            userWalletsListManager.unlockWithCard(userWallet)
                .doOnFailure { error ->
                    store.dispatchOnMain(WelcomeAction.ProceedWithCard.Error(error))
                }
                .doOnSuccess {
                    store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                    store.dispatchOnMain(WelcomeAction.ProceedWithCard.Success)
                    store.onUserWalletSelected(userWallet)
                }
        }
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