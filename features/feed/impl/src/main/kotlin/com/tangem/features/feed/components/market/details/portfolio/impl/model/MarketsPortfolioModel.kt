package com.tangem.features.feed.components.market.details.portfolio.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioManager
import com.tangem.features.feed.components.market.details.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.impl.analytics.PortfolioAnalyticsEvent
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.state.TokenActionsBSContentUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class MarketsPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    private val marketsPortfolioDelegateFactory: MarketsPortfolioDelegate.Factory,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val tokenActionsHandlerFactory: TokenActionsHandler.Factory,
    private val receiveAddressesFactory: ReceiveAddressesFactory,
    private val analyticsEventHandler: AnalyticsEventHandler,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val state: StateFlow<MyPortfolioUM>
        field = MutableStateFlow<MyPortfolioUM>(value = MyPortfolioUM.Loading)

    private val params = paramsContainer.require<MarketsPortfolioComponent.Params>()

    private val analyticsEventBuilder = PortfolioAnalyticsEvent.EventBuilder(
        tokenSymbol = params.token.symbol,
        source = params.analyticsParams?.source,
    )

    private val currentAppCurrency = createAppCurrencyFlow()
    private val tokenActionsHandler = createTokenActionsHandler()

    val addToPortfolioManager: AddToPortfolioManager = createAddToPortfolioManager()
    private val marketsPortfolioDelegate: MarketsPortfolioDelegate = createMarketsPortfolioDelegate()

    val bottomSheetNavigation: SlotNavigation<MarketsPortfolioRoute> = SlotNavigation()
    val addToPortfolioCallback = object : AddToPortfolioComponent.Callback {
        override fun onDismiss() = bottomSheetNavigation.dismiss()
        override fun onSuccess(addedToken: CryptoCurrency) = bottomSheetNavigation.dismiss()
    }

    init {
        marketsPortfolioDelegate.combineData()
            .onEach { state.value = it }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        addToPortfolioManager.setTokenNetworks(networks)
        marketsPortfolioDelegate.setTokenNetworks(networks)
    }

    fun setNoNetworksAvailable() {
        addToPortfolioManager.setTokenNetworks(emptyList())
        marketsPortfolioDelegate.setTokenNetworks(emptyList())
    }

    private fun createAddToPortfolioManager(): AddToPortfolioManager {
        return addToPortfolioManagerFactory.create(
            scope = modelScope,
            token = params.token,
            analyticsParams = params.analyticsParams?.source?.let { AddToPortfolioManager.AnalyticsParams(it) },
        )
    }

    private fun createMarketsPortfolioDelegate(): MarketsPortfolioDelegate {
        return marketsPortfolioDelegateFactory.create(
            scope = modelScope,
            token = params.token,
            tokenActionsHandler = tokenActionsHandler,
            buttonState = addToPortfolioManager.state.map { state ->
                when (state) {
                    is AddToPortfolioManager.State.AvailableToAdd -> {
                        MyPortfolioUM.Tokens.AddButtonState.Available
                    }
                    AddToPortfolioManager.State.Init -> MyPortfolioUM.Tokens.AddButtonState.Loading
                    AddToPortfolioManager.State.NothingToAdd -> MyPortfolioUM.Tokens.AddButtonState.Unavailable
                }
            },
            onAddClick = {
                analyticsEventHandler.send(analyticsEventBuilder.addToPortfolioClicked())
                bottomSheetNavigation.activate(MarketsPortfolioRoute.AddToPortfolio)
            },
        )
    }

    private fun createAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map { maybeAppCurrency ->
                maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppCurrency.Default,
            )
    }

    private fun createTokenActionsHandler(): TokenActionsHandler {
        return tokenActionsHandlerFactory.create(
            currentAppCurrency = Provider { currentAppCurrency.value },
            onHandleQuickAction = { handledAction ->
                val currency = handledAction.cryptoCurrencyData.status.currency
                analyticsEventHandler.send(
                    analyticsEventBuilder.quickActionClick(
                        actionUM = handledAction.action,
                        blockchainName = currency.network.name,
                    ),
                )
                configureReceiveAddresses(handledAction)
            },
        )
    }

    private fun configureReceiveAddresses(quickAction: TokenActionsHandler.HandledQuickAction) {
        val isNewReceive = quickAction.action == TokenActionsBSContentUM.Action.Receive
        if (isNewReceive) {
            modelScope.launch {
                val tokenConfig = receiveAddressesFactory.create(
                    status = quickAction.cryptoCurrencyData.status,
                    userWalletId = quickAction.cryptoCurrencyData.userWallet.walletId,
                ) ?: return@launch
                bottomSheetNavigation.activate(MarketsPortfolioRoute.TokenReceive(tokenConfig))
            }
        }
    }
}