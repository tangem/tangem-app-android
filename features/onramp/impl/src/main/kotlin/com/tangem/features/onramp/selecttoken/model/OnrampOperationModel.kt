package com.tangem.features.onramp.selecttoken.model

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.alerts.models.AlertDemoModeUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.models.requireColdWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selecttoken.OnrampOperationComponent.Params
import com.tangem.features.onramp.selecttoken.entity.OnrampOperationUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class OnrampOperationModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getWalletsUseCase: GetWalletsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: AppRouter,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val messageSender: UiMessageSender,
    private val rampStateManager: RampStateManager,
) : Model() {

    val state: StateFlow<OnrampOperationUM> get() = _state

    private val params: Params = paramsContainer.require()

    private val _state = MutableStateFlow(value = getInitialState())

    private val selectedUserWallet = getWalletsUseCase.invokeSync()
        .first { it.walletId == params.userWalletId }
        .requireColdWallet()

    init {
        analyticsEventHandler.send(
            event = when (params) {
                is Params.Buy -> MainScreenAnalyticsEvent.BuyScreenOpened
                is Params.Sell -> MainScreenAnalyticsEvent.SellScreenOpened
            },
        )
    }

    fun onTokenClick(status: CryptoCurrencyStatus) {
        val currencySymbol = status.currency.symbol
        analyticsEventHandler.send(
            event = when (params) {
                is Params.Buy -> MainScreenAnalyticsEvent.BuyTokenClicked(currencySymbol = currencySymbol)
                is Params.Sell -> MainScreenAnalyticsEvent.SellTokenClicked(currencySymbol = currencySymbol)
            },
        )

        selectTokenIfDemoModeOff(status)
    }

    fun onHotTokenClick(status: CryptoCurrencyStatus) {
        modelScope.launch {
            val unavailabilityReason = rampStateManager.availableForBuy(
                userWallet = selectedUserWallet,
                cryptoCurrency = status.currency,
            )

            if (unavailabilityReason == ScenarioUnavailabilityReason.None) {
                analyticsEventHandler.send(
                    event = MainScreenAnalyticsEvent.HotTokenClicked(currencySymbol = status.currency.symbol),
                )

                selectTokenIfDemoModeOff(status)
            }
        }
    }

    private fun selectTokenIfDemoModeOff(status: CryptoCurrencyStatus) {
        if (params is Params.Sell) {
            showErrorIfDemoModeOrElse { selectToken(status) }
        } else {
            selectToken(status)
        }
    }

    private fun selectToken(status: CryptoCurrencyStatus) {
        modelScope.launch {
            when (params) {
                is Params.Buy -> {
                    router.push(
                        AppRoute.Onramp(
                            userWalletId = selectedUserWallet.walletId,
                            currency = status.currency,
                            source = OnrampSource.ACTION_BUTTONS,
                        ),
                    )
                }
                is Params.Sell -> {
                    val appCurrencyCode = getSelectedAppCurrencyUseCase.invokeSync()
                        .getOrElse { AppCurrency.Default }.code

                    reduxStateHolder.dispatch(
                        action = TradeCryptoAction.Sell(status, appCurrencyCode),
                    )
                }
            }
        }
    }

    private fun getInitialState(): OnrampOperationUM {
        return OnrampOperationUM(
            titleResId = when (params) {
                is Params.Buy -> R.string.common_buy
                is Params.Sell -> R.string.common_sell
            },
            onBackClick = ::onBackClick,
            isHotCryptoVisible = false,
        )
    }

    private fun onBackClick() {
        analyticsEventHandler.send(
            event = MainScreenAnalyticsEvent.ButtonClose(
                source = when (params) {
                    is Params.Buy -> AnalyticsParam.ScreensSources.Buy
                    is Params.Sell -> AnalyticsParam.ScreensSources.Sell
                },
            ),
        )

        router.pop()
    }

    private fun showErrorIfDemoModeOrElse(action: () -> Unit) {
        if (isDemoCardUseCase(cardId = selectedUserWallet.cardId)) {
            val alertUM = AlertDemoModeUM(onConfirmClick = {})

            val message = DialogMessage(
                title = alertUM.title,
                message = alertUM.message,
                firstActionBuilder = {
                    EventMessageAction(
                        title = alertUM.confirmButtonText,
                        onClick = alertUM.onConfirmClick,
                    )
                },
                secondActionBuilder = { cancelAction() },
            )

            messageSender.send(message)
        } else {
            action()
        }
    }

    fun onTokenListInitialized() {
        // Makes HotCrypto tokens visible when token list is initialized to synchronize UI
        _state.update {
            it.copy(isHotCryptoVisible = true)
        }
    }
}