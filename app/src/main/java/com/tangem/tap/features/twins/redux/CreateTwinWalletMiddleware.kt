package com.tangem.tap.features.twins.redux

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

class CreateTwinWalletMiddleware {
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
    val action = action as? CreateTwinWalletAction ?: return

    when (action) {
        is CreateTwinWalletAction.ShowWarning -> {
            val wallet = store.state.detailsState.wallets.firstOrNull()
            if (wallet == null) {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWalletWarning))
                return
            }
            val notEmpty = wallet.recentTransactions.isNotEmpty() || wallet.amounts.toSendableAmounts().isNotEmpty()
            if (notEmpty) {
                store.dispatch(CreateTwinWalletAction.NotEmpty)
            } else {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWalletWarning))
            }
        }
        is CreateTwinWalletAction.Proceed ->
            store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWallet))
        is CreateTwinWalletAction.Cancel -> {

            val step = store.state.detailsState.createTwinWalletState?.step
            if (step != null && step != CreateTwinWalletStep.FirstStep
            ) {
                store.dispatch(CreateTwinWalletAction.ShowAlert)
            } else {
                twinsManager = null
                store.dispatch(NavigationAction.PopBackTo())
            }
        }
        is CreateTwinWalletAction.Cancel.Confirm -> {
            twinsManager = null
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
        }
        is CreateTwinWalletAction.LaunchFirstStep -> {
            store.state.globalState.scanResponse?.let {
                twinsManager = TwinCardsManager(it, action.context)
            }
            scope.launch {
                val result = twinsManager?.createFirstWallet(action.message)
                withContext(Dispatchers.Main) {
                    when (result) {
                        SimpleResult.Success ->
                            store.dispatch(CreateTwinWalletAction.LaunchFirstStep.Success)
                        is SimpleResult.Failure ->
                            store.dispatch(CreateTwinWalletAction.LaunchFirstStep.Failure)
                    }
                }
            }
        }
        CreateTwinWalletAction.LaunchFirstStep.Success -> {

        }
        CreateTwinWalletAction.LaunchFirstStep.Failure -> {

        }
        is CreateTwinWalletAction.LaunchSecondStep ->
            scope.launch {
                val result = twinsManager?.createSecondWallet(action.initialMessage,
                    action.preparingMessage, action.creatingWalletMessage)
                withContext(Dispatchers.Main) {
                    when (result) {
                        SimpleResult.Success ->
                            store.dispatch(CreateTwinWalletAction.LaunchSecondStep.Success)
                        is SimpleResult.Failure ->
                            store.dispatch(CreateTwinWalletAction.LaunchSecondStep.Failure)
                    }
                }
            }
        CreateTwinWalletAction.LaunchSecondStep.Success -> {

        }
        CreateTwinWalletAction.LaunchSecondStep.Failure -> {

        }
        is CreateTwinWalletAction.LaunchThirdStep -> {
            scope.launch {
                val result = twinsManager?.complete(action.message)
                withContext(Dispatchers.Main) {
                    when (result) {
                        is Result.Success ->
                            store.dispatch(
                                CreateTwinWalletAction
                                        .LaunchThirdStep.Success(result.data)
                            )
                        is Result.Failure ->
                            store.dispatch(CreateTwinWalletAction.LaunchThirdStep.Failure)
                    }
                }
            }
        }
        is CreateTwinWalletAction.LaunchThirdStep.Success -> {
            scope.launch {
                store.state.globalState.tapWalletManager.onCardScanned(action.scanResponse)
            }
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
        }
        CreateTwinWalletAction.LaunchThirdStep.Failure -> {

        }
    }
}