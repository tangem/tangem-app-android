package com.tangem.tap.features.details.redux.twins

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateTwinWalletMiddleware {
    var twinsManager: TwinCardsManager? = null
    fun handle(action: DetailsAction.CreateTwinWalletAction) {
        when (action) {
            is DetailsAction.CreateTwinWalletAction.ShowWarning -> {
                val wallet = store.state.detailsState.wallets.firstOrNull()
                if (wallet == null) {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWalletWarning))
                    return
                }
                val notEmpty = wallet.recentTransactions.isNotEmpty() || wallet.amounts.toSendableAmounts().isNotEmpty()
                if (notEmpty) {
                    store.dispatch(DetailsAction.CreateTwinWalletAction.NotEmpty)
                } else {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWalletWarning))
                }
            }
            is DetailsAction.CreateTwinWalletAction.Proceed ->
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CreateTwinWallet))
            is DetailsAction.CreateTwinWalletAction.Cancel -> {

                val step = store.state.detailsState.createTwinWalletState?.step
                if (step != null && step != CreateTwinWalletStep.FirstStep
                ) {
                    store.dispatch(DetailsAction.CreateTwinWalletAction.ShowAlert)
                } else {
                    twinsManager = null
                    store.dispatch(NavigationAction.PopBackTo())
                }
            }
            is DetailsAction.CreateTwinWalletAction.Cancel.Confirm -> {
                twinsManager = null
                store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
            }
            is DetailsAction.CreateTwinWalletAction.LaunchFirstStep -> {
                store.state.globalState.scanNoteResponse?.let {
                    twinsManager = TwinCardsManager(it, action.context)
                }
                scope.launch {
                    val result = twinsManager?.createFirstWallet(action.message)
                    withContext(Dispatchers.Main) {
                        when (result) {
                            SimpleResult.Success ->
                                store.dispatch(DetailsAction.CreateTwinWalletAction.LaunchFirstStep.Success)
                            is SimpleResult.Failure ->
                                store.dispatch(DetailsAction.CreateTwinWalletAction.LaunchFirstStep.Failure)
                        }
                    }
                }
            }
            DetailsAction.CreateTwinWalletAction.LaunchFirstStep.Success -> {

            }
            DetailsAction.CreateTwinWalletAction.LaunchFirstStep.Failure -> {

            }
            is DetailsAction.CreateTwinWalletAction.LaunchSecondStep ->
                scope.launch {
                    val result = twinsManager?.createSecondWallet(action.initialMessage,
                            action.preparingMessage, action.creatingWalletMessage)
                    withContext(Dispatchers.Main) {
                        when (result) {
                            SimpleResult.Success ->
                                store.dispatch(DetailsAction.CreateTwinWalletAction.LaunchSecondStep.Success)
                            is SimpleResult.Failure ->
                                store.dispatch(DetailsAction.CreateTwinWalletAction.LaunchSecondStep.Failure)
                        }
                    }
                }
            DetailsAction.CreateTwinWalletAction.LaunchSecondStep.Success -> {

            }
            DetailsAction.CreateTwinWalletAction.LaunchSecondStep.Failure -> {

            }
            is DetailsAction.CreateTwinWalletAction.LaunchThirdStep -> {
                scope.launch {
                    val result = twinsManager?.complete(action.message)
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is Result.Success ->
                                store.dispatch(
                                        DetailsAction.CreateTwinWalletAction
                                                .LaunchThirdStep.Success(result.data)
                                )
                            is Result.Failure ->
                                store.dispatch(DetailsAction.CreateTwinWalletAction.LaunchThirdStep.Failure)
                        }
                    }
                }
            }
            is DetailsAction.CreateTwinWalletAction.LaunchThirdStep.Success -> {
                scope.launch {
                    store.state.globalState.tapWalletManager.onCardScanned(action.scanNoteResponse)
                }
                store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
            }
            DetailsAction.CreateTwinWalletAction.LaunchThirdStep.Failure -> {

            }
        }
    }
}