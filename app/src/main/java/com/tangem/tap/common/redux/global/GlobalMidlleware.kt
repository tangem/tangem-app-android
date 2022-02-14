package com.tangem.tap.common.redux.global

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.services.Result
import com.tangem.tap.*
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.withMainContext
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.moonpay.MoonpayService
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber

class GlobalMiddleware {
    companion object {
        val handler = globalMiddlewareHandler
    }
}

private val globalMiddlewareHandler: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
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
                        preferencesStorage.getAppCurrency()
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
                is GlobalAction.GetMoonPayStatus -> {
                    val apiKey = appState()?.globalState?.configManager?.config?.moonPayApiKey
                    if (apiKey != null) {
                        scope.launch {
                            val result = MoonpayService().getMoonpayStatus(apiKey)
                            when (result) {
                                is Result.Success -> {
                                    store.dispatchOnMain(GlobalAction.GetMoonPayStatus.Success(result.data))
                                }
                                is Result.Failure -> Timber.e(result.error)
                            }
                        }
                    }
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
                                is CompletionResult.Failure -> action.onFailure?.invoke(result.error)
                            }
                        }
                    }
                }
            }
            nextDispatch(action)
        }
    }
}