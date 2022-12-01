package com.tangem.tap.common.redux.global

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.ifNotNull
import com.tangem.common.services.Result
import com.tangem.domain.common.LogConfig
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.exchangeServices.CardExchangeRules
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoApi
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoService
import com.tangem.tap.network.exchangeServices.moonpay.MoonPayService
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userTokensRepository
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware
import java.util.*

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
            store.dispatch(
                GlobalAction.RestoreAppCurrency.Success(
                    preferencesStorage.fiatCurrenciesPrefStorage.getAppCurrency(),
                ),
            )
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
        is GlobalAction.SendEmail -> {
            store.state.globalState.feedbackManager?.sendEmail(action.feedbackData)
        }
        is GlobalAction.OpenChat -> {
            val globalState = store.state.globalState
            val feedbackManager = globalState.feedbackManager.guard {
                store.dispatchDebugErrorNotification("FeedbackManager not initialized")
                return
            }
            val config = globalState.configManager?.config.guard {
                store.dispatchDebugErrorNotification("Config not initialized")
                return
            }

            val scanResponse = globalState.scanResponse ?: globalState.onboardingState.onboardingManager?.scanResponse

            // if config not set -> try to get it based on a scanResponse.productType
            val unsafeZendeskConfig = action.zendeskConfig ?: when {
                scanResponse?.isSaltPay() == true -> config.saltPayConfig?.zendesk
                else -> config.zendesk
            }

            val zendeskConfig = unsafeZendeskConfig.guard {
                store.dispatchDebugErrorNotification("ZendeskConfig not initialized")
                return
            }
            feedbackManager.initChat(zendeskConfig)
            feedbackManager.openChat(action.feedbackData)
        }
        is GlobalAction.UpdateWalletSignedHashes -> {
            store.dispatch(WalletAction.Warnings.CheckRemainingSignatures(action.remainingSignatures))
        }
        is GlobalAction.UpdateFeedbackInfo -> {
            store.state.globalState.feedbackManager?.infoHolder
                ?.setWalletsInfo(action.walletManagers)
        }
        is GlobalAction.ExchangeManager.Init -> {
            val appStateSafe = appState() ?: return
            val config = appStateSafe.globalState.configManager?.config
            ifNotNull(
                config?.mercuryoWidgetId,
                config?.mercuryoSecret,
                config?.moonPayApiKey,
                config?.moonPayApiSecretKey,
            ) { mercuryoWidgetId, mercuryoSecret, moonPayKey, moonPaySecretKey ->
                scope.launch {
                    val buyService = MercuryoService(
                        apiVersion = MercuryoApi.API_VERSION,
                        mercuryoWidgetId = mercuryoWidgetId,
                        secret = mercuryoSecret,
                        logEnabled = LogConfig.network.mercuryoService,
                    )
                    val sellService = MoonPayService(
                        apiKey = moonPayKey,
                        secretKey = moonPaySecretKey,
                        logEnabled = LogConfig.network.moonPayService,
                    )
                    val cardProvider = {
                        store.state.globalState.scanResponse?.card
                            ?: store.state.globalState.onboardingState.onboardingManager?.scanResponse?.card
                    }

                    val exchangeManager = CurrencyExchangeManager(
                        buyService = buyService,
                        sellService = sellService,
                        primaryRules = CardExchangeRules(cardProvider),
                    )
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
                tangemSdkManager.changeDisplayedCardIdNumbersCount(null)
                val result = tangemSdkManager.scanProduct(
                    userTokensRepository = userTokensRepository,
                    additionalBlockchainsToDerive = action.additionalBlockchainsToDerive,
                    messageRes = action.messageResId,
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
        is GlobalAction.FetchUserCountry -> {
            scope.launch {
                val techService = store.state.domainNetworks.tangemTechService
                when (val result = techService.userCountry()) {
                    is Result.Success -> {
                        store.dispatchOnMain(
                            GlobalAction.FetchUserCountry.Success(
                                countryCode = result.data.code.lowercase(),
                            ),
                        )
                    }
                    is Result.Failure -> {
                        store.dispatchOnMain(
                            GlobalAction.FetchUserCountry.Success(
                                countryCode = Locale.getDefault().country.lowercase(),
                            ),
                        )
                    }
                }
            }
        }
    }
}
