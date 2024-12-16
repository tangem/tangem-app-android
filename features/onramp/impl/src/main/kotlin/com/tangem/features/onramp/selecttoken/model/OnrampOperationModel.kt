package com.tangem.features.onramp.selecttoken.model

import androidx.compose.ui.res.stringResource
import arrow.core.getOrElse
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.alerts.models.AlertDemoModeUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.message.ContentMessage
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.OnrampFeatureToggles
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selecttoken.OnrampOperationComponent.Params
import com.tangem.features.onramp.selecttoken.entity.OnrampOperationUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ComponentScoped
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
    private val onrampFeatureToggles: OnrampFeatureToggles,
) : Model() {

    val state: StateFlow<OnrampOperationUM> get() = _state

    private val params: Params = paramsContainer.require()

    private val _state = MutableStateFlow(value = getInitialState())

    private val selectedUserWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }

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

        if (params is Params.Sell || !onrampFeatureToggles.isFeatureEnabled) {
            showErrorIfDemoModeOrElse { selectToken(status) }
        } else {
            selectToken(status)
        }
    }

    private fun selectToken(status: CryptoCurrencyStatus) {
        modelScope.launch {
            val appCurrencyCode = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }.code

            reduxStateHolder.dispatch(
                action = when (params) {
                    is Params.Buy -> getBuyAction(status, appCurrencyCode)
                    is Params.Sell -> TradeCryptoAction.Sell(status, appCurrencyCode)
                },
            )
        }
    }

    private fun getInitialState(): OnrampOperationUM {
        return OnrampOperationUM(
            titleResId = when (params) {
                is Params.Buy -> R.string.common_buy
                is Params.Sell -> R.string.common_sell
            },
            onBackClick = ::onBackClick,
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

    private fun getBuyAction(status: CryptoCurrencyStatus, appCurrencyCode: String): TradeCryptoAction {
        return TradeCryptoAction.Buy(
            userWallet = selectedUserWallet,
            cryptoCurrencyStatus = status,
            source = OnrampSource.ACTION_BUTTONS,
            appCurrencyCode = appCurrencyCode,
        )
    }

    private fun showErrorIfDemoModeOrElse(action: () -> Unit) {
        if (isDemoCardUseCase(cardId = selectedUserWallet.cardId)) {
            val alertUM = AlertDemoModeUM(onConfirmClick = {})

            messageSender.send(
                message = ContentMessage { onDismiss ->
                    val confirmButton = DialogButtonUM(
                        title = alertUM.confirmButtonText.resolveReference(),
                        onClick = {
                            alertUM.onConfirmClick()
                            onDismiss()
                        },
                    )

                    val dismissButton = DialogButtonUM(
                        title = stringResource(id = R.string.common_cancel),
                        onClick = onDismiss,
                    )

                    BasicDialog(
                        message = alertUM.message.resolveReference(),
                        confirmButton = confirmButton,
                        onDismissDialog = onDismiss,
                        title = alertUM.title.resolveReference(),
                        dismissButton = dismissButton,
                    )
                },
            )
        } else {
            action()
        }
    }
}