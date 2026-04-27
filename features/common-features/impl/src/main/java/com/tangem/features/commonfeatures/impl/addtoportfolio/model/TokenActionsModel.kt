package com.tangem.features.commonfeatures.impl.addtoportfolio.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.common.ui.markets.action.TokenActionsHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.features.commonfeatures.impl.addtoportfolio.TokenActionsComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.ui.state.TokenActionsUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class TokenActionsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
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
            onHandleQuickAction = { handledAction -> handledQuickAction(handledAction) },
        )

    val bottomSheetNavigation: SlotNavigation<TokenReceiveConfig> = SlotNavigation()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TokenActionsUM?> =
        combine(
            params.data,
            getBalanceHidingSettingsUseCase.isBalanceHidden(),
        ) { cryptoCurrencyData, isBalanceHidden ->
            cryptoCurrencyData to isBalanceHidden
        }
            .mapLatest { (cryptoCurrencyData, isBalanceHidden) ->
                uiBuilder.build(
                    cryptoCurrencyData = cryptoCurrencyData,
                    tokenActionsHandler = tokenActionsHandler,
                    eventBuilder = analyticsEventBuilder.first(),
                    appCurrency = currentAppCurrency.value,
                    isBalanceHidden = isBalanceHidden,
                )
            }
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    private fun handledQuickAction(handledAction: TokenActionsHandler.HandledQuickAction) = modelScope.launch {
        val event = analyticsEventBuilder.first().getTokenActionClick(actionUM = handledAction.action)
        analyticsEventHandler.send(event)
        val isReceive = handledAction.action == TokenActionsBSContentUM.Action.Receive
        if (!isReceive) return@launch
        modelScope.launch {
            val tokenConfig = receiveAddressesFactory.create(
                status = handledAction.cryptoCurrencyData.status,
                userWalletId = handledAction.cryptoCurrencyData.userWallet.walletId,
            ) ?: return@launch
            bottomSheetNavigation.activate(tokenConfig)
        }
    }
}