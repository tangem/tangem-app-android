package com.tangem.tap.common.redux.global

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.LogConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.StateDialog
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
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
        is GlobalAction.ExchangeManager.Init -> {
            val config = store.inject(DaggerGraphState::environmentConfigStorage).getConfigSync()

            scope.launch {
                val scanResponseProvider: () -> ScanResponse? = {
                    val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
                    userWalletsListManager.selectedUserWalletSync?.scanResponse
                }
                val cardProvider: () -> CardDTO? = { scanResponseProvider.invoke()?.card }

                val buyService = makeBuyExchangeService(config)
                val sellService = makeSellExchangeService(config)
                val exchangeManager = CurrencyExchangeManager(
                    buyService = buyService,
                    sellService = sellService,
                    primaryRules = CardExchangeRules(cardProvider),
                )
                // TODO: for refactoring (after remove old design refactor CurrencyExchangeManager and use 1 instance)
                store.inject(DaggerGraphState::appStateHolder).buyService = buyService
                store.inject(DaggerGraphState::appStateHolder).sellService = sellService
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
            val homeFeatureToggles = store.inject(DaggerGraphState::homeFeatureToggles)

            if (!homeFeatureToggles.isMigrateUserCountryCodeEnabled) {
                scope.launch {
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

private fun makeSellExchangeService(environmentConfig: EnvironmentConfig): ExchangeService {
    return MoonPayService(
        apiKey = environmentConfig.moonPayApiKey,
        secretKey = environmentConfig.moonPayApiSecretKey,
        logEnabled = LogConfig.network.moonPayService,
    )
}

private fun makeBuyExchangeService(environmentConfig: EnvironmentConfig): ExchangeService {
    return MercuryoService(
        environment = MercuryoEnvironment.prod(
            widgetId = environmentConfig.mercuryoWidgetId,
            secret = environmentConfig.mercuryoSecret,
        ),
    )
}