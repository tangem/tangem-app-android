package com.tangem.tap.features.twins.redux

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
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

private var twinsManager: TwinCardsManager? = null

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
        is TwinCardsAction.CreateWallet.Create -> {
            val wallet = store.state.walletState.walletManagers.map { it.wallet }.firstOrNull()
            if (wallet == null) {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWalletWarning))
                return
            }
            val notEmpty = wallet.recentTransactions.isNotEmpty() || wallet.amounts.toSendableAmounts().isNotEmpty()
            if (notEmpty) {
                store.dispatch(TwinCardsAction.CreateWallet.NotEmpty)
            } else {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWalletWarning))
            }
        }
        is TwinCardsAction.CreateWallet.Proceed -> {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWallet))
        }
        is TwinCardsAction.CreateWallet.Cancel -> {
            val step = twinCardsState.createWalletState?.step
            if (step != null && step != CreateTwinWalletStep.FirstStep) {
                store.dispatch(TwinCardsAction.CreateWallet.ShowAlert)
            } else {
                twinsManager = null
                store.dispatch(NavigationAction.PopBackTo())
            }
        }
        is TwinCardsAction.CreateWallet.Cancel.Confirm -> {
            twinsManager = null
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
        }
        is TwinCardsAction.CreateWallet.LaunchFirstStep -> {
            store.state.globalState.scanResponse?.let {
                twinsManager = TwinCardsManager(it, action.context)
            }
            scope.launch {
                val result = twinsManager?.createFirstWallet(action.message)
                withContext(Dispatchers.Main) {
                    when (result) {
                        SimpleResult.Success ->
                            store.dispatch(TwinCardsAction.CreateWallet.LaunchFirstStep.Success)
                        is SimpleResult.Failure ->
                            store.dispatch(TwinCardsAction.CreateWallet.LaunchFirstStep.Failure)
                    }
                }
            }
        }
        TwinCardsAction.CreateWallet.LaunchFirstStep.Success -> {

        }
        TwinCardsAction.CreateWallet.LaunchFirstStep.Failure -> {

        }
        is TwinCardsAction.CreateWallet.LaunchSecondStep ->
            scope.launch {
                val result = twinsManager?.createSecondWallet(action.initialMessage,
                    action.preparingMessage, action.creatingWalletMessage)
                withContext(Dispatchers.Main) {
                    when (result) {
                        SimpleResult.Success ->
                            store.dispatch(TwinCardsAction.CreateWallet.LaunchSecondStep.Success)
                        is SimpleResult.Failure ->
                            store.dispatch(TwinCardsAction.CreateWallet.LaunchSecondStep.Failure)
                    }
                }
            }
        TwinCardsAction.CreateWallet.LaunchSecondStep.Success -> {

        }
        TwinCardsAction.CreateWallet.LaunchSecondStep.Failure -> {

        }
        is TwinCardsAction.CreateWallet.LaunchThirdStep -> {
            scope.launch {
                val result = twinsManager?.complete(action.message)
                withContext(Dispatchers.Main) {
                    when (result) {
                        is Result.Success ->
                            store.dispatch(TwinCardsAction.CreateWallet.LaunchThirdStep.Success(result.data))
                        is Result.Failure ->
                            store.dispatch(TwinCardsAction.CreateWallet.LaunchThirdStep.Failure)
                    }
                }
            }
        }
        is TwinCardsAction.CreateWallet.LaunchThirdStep.Success -> {
            scope.launch {
                store.state.globalState.tapWalletManager.onCardScanned(action.scanResponse)
            }
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
        }
        TwinCardsAction.CreateWallet.LaunchThirdStep.Failure -> {

        }
    }
}