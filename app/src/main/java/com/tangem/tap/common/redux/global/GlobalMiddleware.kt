package com.tangem.tap.common.redux.global

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.datasource.config.models.Config
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.LogConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.StateDialog
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.network.exchangeServices.BuyExchangeService
import com.tangem.tap.network.exchangeServices.CardExchangeRules
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoEnvironment
import com.tangem.tap.network.exchangeServices.mercuryo.MercuryoService
import com.tangem.tap.network.exchangeServices.moonpay.MoonPayService
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import java.util.Locale

object GlobalMiddleware {
    val handler = globalMiddlewareHandler
}

private val globalMiddlewareHandler: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            handleAction(action, appState)
            nextDispatch(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod")
private fun handleAction(action: Action, appState: () -> AppState?) {
    when (action) {
        is GlobalAction.ScanFailsCounter.ChooseBehavior -> {
            when (action.result) {
                is CompletionResult.Success -> store.dispatch(GlobalAction.ScanFailsCounter.Reset)
                is CompletionResult.Failure -> {
                    handleFailureChooseBehaviour(action.result, action.analyticsSource)
                }
            }
        }
        is GlobalAction.RestoreAppCurrency -> {
            restoreAppCurrency()
        }
        is GlobalAction.HideWarningMessage -> {
            store.state.globalState.warningManager?.let {
                if (it.hideWarning(action.warning)) {
                    // if (WarningMessagesManager.isAlreadySignedHashesWarning()) {
                    //     // TODO: No appropriate warningMessage identification. Make it better later
                    //     store.dispatch(WalletAction.Warnings.CheckHashesCount.SaveCardId)
                    // }

                    store.dispatch(SendAction.Warnings.Update)
                }
            }
        }
        is GlobalAction.SendEmail -> {
            store.state.globalState.feedbackManager?.sendEmail(
                feedbackData = action.feedbackData,
                scanResponse = action.scanResponse,
            )
        }
        is GlobalAction.ExchangeManager.Init -> {
            val appStateSafe = appState() ?: return
            val config = appStateSafe.globalState.configManager?.config ?: return

            scope.launch {
                val scanResponseProvider: () -> ScanResponse? = {
                    val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
                    userWalletsListManager.selectedUserWalletSync?.scanResponse
                }
                val cardProvider: () -> CardDTO? = { scanResponseProvider.invoke()?.card }

                val exchangeManager = CurrencyExchangeManager(
                    buyService = makeBuyExchangeService(config),
                    sellService = makeSellExchangeService(config),
                    primaryRules = CardExchangeRules(cardProvider),
                )
                // TODO: for refactoring (after remove old design refactor CurrencyExchangeManager and use 1 instance)
                store.inject(DaggerGraphState::appStateHolder).exchangeService = exchangeManager
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
    }
}

private fun handleFailureChooseBehaviour(
    result: CompletionResult.Failure<ScanResponse>,
    analyticsSource: AnalyticsParam.ScreensSources,
) {
    if (result.error is TangemSdkError.UserCancelled) {
        store.dispatch(GlobalAction.ScanFailsCounter.Increment)
        if (store.state.globalState.scanCardFailsCounter >= 2) {
            val scanFailsSource = when (analyticsSource) {
                is AnalyticsParam.ScreensSources.SignIn -> StateDialog.ScanFailsSource.SIGN_IN
                is AnalyticsParam.ScreensSources.Settings -> StateDialog.ScanFailsSource.SETTINGS
                is AnalyticsParam.ScreensSources.Intro -> StateDialog.ScanFailsSource.INTRO
                else -> StateDialog.ScanFailsSource.MAIN
            }
            store.dispatchDialogShow(StateDialog.ScanFailsDialog(scanFailsSource))
        }
    } else {
        store.dispatch(GlobalAction.ScanFailsCounter.Reset)
    }
}

private fun restoreAppCurrency() {
    scope.launch {
        val currency = store.inject(DaggerGraphState::appCurrencyRepository)
            .getSelectedAppCurrency()
            .firstOrNull()
            ?: AppCurrency.Default

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