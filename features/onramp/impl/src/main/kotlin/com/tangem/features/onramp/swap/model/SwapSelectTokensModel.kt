package com.tangem.features.onramp.swap.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.component.SwapSelectTokensComponent
import com.tangem.features.onramp.swap.entity.SwapSelectTokensController
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUM
import com.tangem.features.onramp.swap.entity.transformer.RemoveSelectedFromTokenTransformer
import com.tangem.features.onramp.swap.entity.transformer.RemoveSelectedToTokenTransformer
import com.tangem.features.onramp.swap.entity.transformer.SelectFromTokenTransformer
import com.tangem.features.onramp.swap.entity.transformer.SelectToTokenTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@Suppress("LongParameterList")
internal class SwapSelectTokensModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val controller: SwapSelectTokensController,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : Model() {

    val state: StateFlow<SwapSelectTokensUM> = controller.state

    val fromCurrencyStatus: StateFlow<CryptoCurrencyStatus?> get() = _fromCurrencyStatus

    private val _fromCurrencyStatus = MutableStateFlow<CryptoCurrencyStatus?>(value = null)
    private val _toCurrencyStatus = MutableStateFlow<CryptoCurrencyStatus?>(value = null)

    private val params = paramsContainer.require<SwapSelectTokensComponent.Params>()

    init {
        controller.update { it.copy(onBackClick = ::onBackClick) }

        subscribeOnBalanceHidingSettings()
    }

    /**
     * Select "from" token
     *
     * @param selectedTokenItemState selected token item state
     * @param status                 crypto currency status
     */
    fun selectFromToken(selectedTokenItemState: TokenItemState, status: CryptoCurrencyStatus) {
        analyticsEventHandler.send(
            event = MainScreenAnalyticsEvent.SwapTokenClicked(currencySymbol = status.currency.symbol),
        )

        _fromCurrencyStatus.value = status

        controller.update(
            transformer = SelectFromTokenTransformer(
                selectedTokenItemState = selectedTokenItemState,
                onRemoveClick = ::onRemoveFromTokenClick,
            ),
        )
    }

    /**
     * Select "to" token
     *
     * @param selectedTokenItemState selected token item state
     * @param status                 crypto currency status
     */
    fun selectToToken(selectedTokenItemState: TokenItemState, status: CryptoCurrencyStatus) {
        analyticsEventHandler.send(
            event = MainScreenAnalyticsEvent.ReceiveTokenClicked(currencySymbol = status.currency.symbol),
        )

        modelScope.launch {
            _toCurrencyStatus.value = status

            controller.update(transformer = SelectToTokenTransformer(selectedTokenItemState))

            // require some delay to show state with selected "from" and "to" tokens
            delay(timeMillis = 500)

            router.push(
                route = AppRoute.Swap(
                    currencyFrom = requireNotNull(fromCurrencyStatus.value).currency,
                    currencyTo = status.currency,
                    userWalletId = params.userWalletId,
                    screenSource = AnalyticsParam.ScreensSources.Main.value,
                ),
                onComplete = {
                    modelScope.launch {
                        withTimeout(timeMillis = 500) {
                            // Return a state with selected only "from" token
                            removeSelectedToToken()
                        }
                    }
                },
            )
        }
    }

    private fun subscribeOnBalanceHidingSettings() {
        getBalanceHidingSettingsUseCase()
            .map { it.isBalanceHidden }
            .distinctUntilChanged()
            .onEach {
                controller.update { state -> state.copy(isBalanceHidden = it) }
            }
            .flowOn(dispatchers.mainImmediate)
            .launchIn(modelScope)
    }

    private fun onBackClick() {
        analyticsEventHandler.send(
            event = MainScreenAnalyticsEvent.ButtonClose(source = AnalyticsParam.ScreensSources.Swap),
        )

        router.pop()
    }

    private fun onRemoveFromTokenClick() {
        val currencySymbol = requireNotNull(_fromCurrencyStatus.value?.currency?.symbol) {
            "Token was not selected"
        }

        analyticsEventHandler.send(
            event = MainScreenAnalyticsEvent.RemoveTokenClicked(currencySymbol = currencySymbol),
        )

        removeSelectedFromToken()
    }

    private fun removeSelectedFromToken() {
        _fromCurrencyStatus.value = null

        controller.update(transformer = RemoveSelectedFromTokenTransformer)
    }

    private fun removeSelectedToToken() {
        _toCurrencyStatus.value = null

        controller.update(
            transformer = RemoveSelectedToTokenTransformer(onRemoveFromTokenClick = ::removeSelectedFromToken),
        )
    }
}