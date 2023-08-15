package com.tangem.tap.common.redux.global

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.datasource.config.models.Config
import com.tangem.domain.common.LogConfig
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.*
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.exchangeServices.BuyExchangeService
import com.tangem.tap.network.exchangeServices.CardExchangeRules
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoEnvironment
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoService
import com.tangem.tap.network.exchangeServices.moonpay.MoonPayService
import com.tangem.tap.proxy.redux.DaggerGraphState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware
import java.util.Locale

object GlobalMiddleware {
    val handler = globalMiddlewareHandler
}

private val globalMiddlewareHandler: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            handleAction(action, appState, dispatch)
            nextDispatch(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod")
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
            if (store.state.daggerGraphState.get(DaggerGraphState::walletFeatureToggles).isRedesignedScreenEnabled) {
                restoreAppCurrencyNew()
            } else {
                restoreAppCurrencyLegacy()
            }
        }
        is GlobalAction.HideWarningMessage -> {
            store.state.globalState.warningManager?.let {
                if (it.hideWarning(action.warning)) {
                    if (WarningMessagesManager.isAlreadySignedHashesWarning(action.warning)) {
                        // TODO: No appropriate warningMessage identification. Make it better later
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

            // if config not set -> try to get it based on a scanResponse.productType
            val unsafeChatConfig = action.chatConfig ?: config.zendesk

            val chatConfig = unsafeChatConfig.guard {
                store.dispatchDebugErrorNotification("ZendeskConfig not initialized")
                return
            }
            feedbackManager.openChat(chatConfig, action.feedbackData)
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
            val config = appStateSafe.globalState.configManager?.config ?: return

            scope.launch {
                val scanResponseProvider: () -> ScanResponse? = {
                    store.state.globalState.scanResponse
                        ?: store.state.globalState.onboardingState.onboardingManager?.scanResponse
                }
                val cardProvider: () -> CardDTO? = { scanResponseProvider.invoke()?.card }

                val exchangeManager = CurrencyExchangeManager(
                    buyService = makeBuyExchangeService(config),
                    sellService = makeSellExchangeService(config),
                    primaryRules = CardExchangeRules(cardProvider),
                )
                store.dispatchOnMain(GlobalAction.ExchangeManager.Init.Success(exchangeManager))
                store.dispatchOnMain(GlobalAction.ExchangeManager.Update)
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
                // TODO("After adding DI") get dependencies by DI
                runCatching { store.state.featureRepositoryProvider.homeRepository.getUserCountryCode() }
                    .onSuccess {
                        store.dispatchOnMain(
                            GlobalAction.FetchUserCountry.Success(countryCode = it.code.lowercase()),
                        )
                    }
                    .onFailure {
                        store.dispatchOnMain(
                            GlobalAction.FetchUserCountry.Success(
                                countryCode = Locale.getDefault().country.lowercase(),
                            ),
                        )
                    }
            }
        }
        is GlobalAction.SetTopUpController -> {
            walletCurrenciesManager.addListener(action.topUpController)
        }
    }
}

private fun restoreAppCurrencyLegacy() {
    store.dispatch(
        GlobalAction.RestoreAppCurrency.Success(
            preferencesStorage.fiatCurrenciesPrefStorage.getAppCurrency()
                ?.run { FiatCurrency(code, name, symbol) }
                ?: FiatCurrency.Default,
        ),
    )
}

private fun restoreAppCurrencyNew() {
    scope.launch {
        val currency = store.state.daggerGraphState.get(DaggerGraphState::appCurrencyRepository)
            .getSelectedAppCurrency()
            .firstOrNull()
            ?.run { FiatCurrency(code, name, symbol) }
            ?: FiatCurrency.Default

        store.dispatchWithMain(GlobalAction.RestoreAppCurrency.Success(currency))
    }
}

private fun makeSellExchangeService(config: Config): ExchangeService {
    return MoonPayService(
        apiKey = config.moonPayApiKey,
        secretKey = config.moonPayApiSecretKey,
        logEnabled = LogConfig.network.moonPayService,
    )
}

private fun makeBuyExchangeService(config: Config): ExchangeService {
    return BuyExchangeService(
        mercuryoService = makeMercuryoExchangeService(config),
    )
}

private fun makeMercuryoExchangeService(config: Config): MercuryoService {
    val mercuryoEnvironment = MercuryoEnvironment.prod(config.mercuryoWidgetId, config.mercuryoSecret)
    return MercuryoService(mercuryoEnvironment)
}