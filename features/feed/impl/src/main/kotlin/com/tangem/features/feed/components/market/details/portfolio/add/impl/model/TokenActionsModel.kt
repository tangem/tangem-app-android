package com.tangem.features.feed.components.market.details.portfolio.add.impl.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.features.feed.components.market.details.portfolio.add.impl.TokenActionsComponent
import com.tangem.features.feed.components.market.details.portfolio.add.impl.ui.state.TokenActionsUM
import com.tangem.features.feed.components.market.details.portfolio.impl.model.TokenActionsHandler
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.state.TokenActionsBSContentUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class TokenActionsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    tokenActionsIntentsFactory: TokenActionsHandler.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val uiBuilder: TokenActionsUiBuilder,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val receiveAddressesFactory: ReceiveAddressesFactory,
) : Model() {

    private val params = paramsContainer.require<TokenActionsComponent.Params>()
    private val analyticsEventBuilder get() = params.eventBuilder
    private val currentAppCurrency = getSelectedAppCurrencyUseCase.invokeOrDefault()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    private val tokenActionsHandler: TokenActionsHandler =
        tokenActionsIntentsFactory.create(
            currentAppCurrency = Provider { currentAppCurrency.value },
            updateTokenReceiveBSConfig = { },
            onHandleQuickAction = { handledAction -> handledQuickAction(handledAction) },
        )

    val bottomSheetNavigation: SlotNavigation<TokenReceiveConfig> = SlotNavigation()
    val uiState: StateFlow<TokenActionsUM?> = params.data
        .mapLatest { uiBuilder.build(it, tokenActionsHandler) }
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    private fun handledQuickAction(handledAction: TokenActionsHandler.HandledQuickAction) {
        val event = analyticsEventBuilder.getTokenActionClick(actionUM = handledAction.action)
        analyticsEventHandler.send(event)
        val isReceive = handledAction.action == TokenActionsBSContentUM.Action.Receive
        if (!isReceive) return
        modelScope.launch {
            val tokenConfig = receiveAddressesFactory.create(
                status = handledAction.cryptoCurrencyData.status,
                userWalletId = handledAction.cryptoCurrencyData.userWallet.walletId,
            ) ?: return@launch
            bottomSheetNavigation.activate(tokenConfig)
        }
    }
}