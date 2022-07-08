package com.tangem.tap.common.redux.global

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.ifNotNull
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.*
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoApi
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoService
import com.tangem.tap.network.exchangeServices.moonpay.MoonPayService
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

class GlobalMiddleware {
    companion object {
        val handler = globalMiddlewareHandler
    }
}

private val globalMiddlewareHandler: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            handleAction(action, appState, dispatch)
            nextDispatch(action)
        }
    }
}

private fun handleAction(action: Action, appState: () -> AppState?, dispatch: DispatchFunction) {
    when (action) {
        is GlobalAction.ScanFailsCounter.ChooseBehavior -> {
            when (action.result) {
                is CompletionResult.Success -> store.dispatch(GlobalAction.ScanFailsCounter.Reset)
                is CompletionResult.Failure -> {
                    if (action.result.error is TangemSdkError.UserCancelled) {
                        store.dispatch(GlobalAction.ScanFailsCounter.Increment)
                        if (store.state.globalState.scanCardFailsCounter >= 2) {
                            store.dispatchDialogShow(AppDialog.ScanFailsDialog)
                        }
                    } else {
                        store.dispatch(GlobalAction.ScanFailsCounter.Reset)
                    }
                }
            }
        }
        is GlobalAction.RestoreAppCurrency -> {
            store.dispatch(GlobalAction.RestoreAppCurrency.Success(
                preferencesStorage.fiatCurrenciesPrefStorage.getAppCurrency()
            ))
        }
        is GlobalAction.HideWarningMessage -> {
            store.state.globalState.warningManager?.let {
                if (it.hideWarning(action.warning)) {
                    if (WarningMessagesManager.isAlreadySignedHashesWarning(action.warning)) {
                        //TODO: No appropriate warningMessage identification. Make it better later
                        store.dispatch(WalletAction.Warnings.CheckHashesCount.SaveCardId)
                    }

                    store.dispatch(WalletAction.Warnings.Update)
                    store.dispatch(SendAction.Warnings.Update)
                }
            }
        }
        is GlobalAction.SendFeedback -> {
            store.state.globalState.feedbackManager?.send(action.emailData)
        }
        is GlobalAction.UpdateWalletSignedHashes -> {
            store.dispatch(WalletAction.Warnings.CheckRemainingSignatures(action.remainingSignatures))
        }
        is GlobalAction.UpdateFeedbackInfo -> {
            store.state.globalState.feedbackManager?.infoHolder
                ?.setWalletsInfo(action.walletManagers)
        }
        is GlobalAction.ExchangeManager.Init -> {
            val config = appState()?.globalState?.configManager?.config
            ifNotNull(
                config?.mercuryoWidgetId,
                config?.moonPayApiKey,
                config?.moonPayApiSecretKey,
            ) { mercuryoWidgetId, moonPayKey, moonPaySecretKey ->
                scope.launch {
                    val buyService = MercuryoService(MercuryoApi.API_VERSION, mercuryoWidgetId)
                    val sellService = MoonPayService(moonPayKey, moonPaySecretKey)
                    val exchangeManager = CurrencyExchangeManager(buyService, sellService)
                    store.dispatchOnMain(GlobalAction.ExchangeManager.Init.Success(exchangeManager))
                    store.dispatchOnMain(GlobalAction.ExchangeManager.Update)
                }
            }
        }
        is GlobalAction.ExchangeManager.Init.Success -> {}
        is GlobalAction.ExchangeManager.Update -> {
            val exchangeManager = appState()?.globalState?.exchangeManager.guard {
                store.dispatchDebugErrorNotification("exchangeManager is not initialized")
                return
            }
            scope.launch { exchangeManager.update() }
        }
        is GlobalAction.ScanCard -> {
            scope.launch {
                val result = tangemSdkManager.scanProduct(
                    store.state.globalState.analyticsHandlers,
                    currenciesRepository,
                    action.additionalBlockchainsToDerive,
                    action.messageResId
                )
                withMainContext {
                    store.dispatch(GlobalAction.ScanFailsCounter.ChooseBehavior(result))
                    when (result) {
                        is CompletionResult.Success -> {
                            tangemSdkManager.changeDisplayedCardIdNumbersCount(result.data)
                            action.onSuccess?.invoke(result.data)
                        }
                        is CompletionResult.Failure -> {
                            action.onFailure?.invoke(result.error)
                        }
                    }
                }
            }
        }
    }
}