package com.tangem.tap.features.twins.redux

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

class TwinCardsMiddleware {
    companion object {
        val handler = twinsWalletMiddleware
    }
}

private val twinsWalletMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handle(action, dispatch)
            next(action)
        }
    }
}

private fun handle(action: Action, dispatch: DispatchFunction) {
    val action = action as? TwinCardsAction ?: return

    val twinCardsState = store.state.twinCardsState

    when (action) {
        is TwinCardsAction.SetTwinCard -> {
            val showOnboarding = !preferencesStorage.wasTwinsOnboardingShown()
            if (showOnboarding) store.dispatch(TwinCardsAction.ShowOnboarding)
        }
        TwinCardsAction.SetOnboardingShown -> {
            preferencesStorage.saveTwinsOnboardingShown()
        }
        is TwinCardsAction.ProceedToCreateWallet -> {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWallet))
        }
        is TwinCardsAction.Wallet.HandleOnBackPressed -> {
            if (twinCardsState.createWalletState?.showAlert == true) {
                val stateDialog = TwinCardsAction.Wallet.InterruptDialog {
                    store.dispatch(TwinCardsAction.CardsManager.Release)
                    store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                }
                store.dispatchDialogShow(stateDialog)
            } else {
                store.dispatch(TwinCardsAction.CardsManager.Release)
                store.dispatch(NavigationAction.PopBackTo())
            }
        }
        is TwinCardsAction.Wallet.Create -> {
            val wallet = store.state.walletState.walletManagers.map { it.wallet }.firstOrNull()
            if (wallet == null) {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWalletWarning))
                return
            }
            val notEmpty = wallet.recentTransactions.isNotEmpty() || wallet.amounts.toSendableAmounts().isNotEmpty()
            if (notEmpty) {
                store.dispatchNotification(twinCardsState.resources.strings.walletIsNotEmpty)
            } else {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWalletWarning))
            }
        }
        is TwinCardsAction.Wallet.LaunchFirstStep -> {
            val scanResponse = store.state.globalState.scanResponse ?: return

            val manager = TwinCardsManager(scanResponse, action.reader)
            store.dispatch(TwinCardsAction.CardsManager.Set(manager))

            scope.launch {
                val result = manager.createFirstWallet(action.initialMessage)
                withContext(Dispatchers.Main) {
                    when (result) {
                        SimpleResult.Success -> {
                            store.dispatch(TwinCardsAction.Wallet.LaunchFirstStep.Success)
                        }
                        is SimpleResult.Failure -> {
                        }
                    }
                }
            }
        }
        is TwinCardsAction.Wallet.LaunchSecondStep -> {
            val manager = twinCardsState.twinCardsManager ?: return

            scope.launch {
                val result = manager.createSecondWallet(action.initialMessage,
                    action.preparingMessage, action.creatingWalletMessage)
                withContext(Dispatchers.Main) {
                    when (result) {
                        SimpleResult.Success -> {
                            store.dispatch(TwinCardsAction.Wallet.LaunchSecondStep.Success)
                        }
                        is SimpleResult.Failure -> {
                        }
                    }
                }
            }
        }
        is TwinCardsAction.Wallet.LaunchThirdStep -> {
            val manager = twinCardsState.twinCardsManager ?: return

            scope.launch {
                val result = manager.complete(action.message)
                withContext(Dispatchers.Main) {
                    when (result) {
                        is Result.Success -> {
                            store.dispatch(TwinCardsAction.Wallet.LaunchThirdStep.Success(result.data))
                        }
                        is Result.Failure -> {

                        }
                    }
                }
            }
        }
        is TwinCardsAction.Wallet.LaunchThirdStep.Success -> {
            scope.launch {
                store.state.globalState.tapWalletManager.onCardScanned(action.scanResponse)
            }
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
        }
    }
}