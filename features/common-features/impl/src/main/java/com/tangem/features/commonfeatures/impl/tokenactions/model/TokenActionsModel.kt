package com.tangem.features.commonfeatures.impl.tokenactions.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.common.ui.markets.action.TokenActionsHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.features.commonfeatures.impl.tokenactions.TokenActionsComponent
import com.tangem.features.commonfeatures.impl.tokenactions.ui.state.TokenActionsUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val receiveAddressesFactory: ReceiveAddressesFactory,
) : Model() {

    private val params = paramsContainer.require<TokenActionsComponent.Params>()
    private val currentAppCurrency = getSelectedAppCurrencyUseCase.invokeOrDefault()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    private val tokenActionsHandler: TokenActionsHandler =
        tokenActionsIntentsFactory.create(
            currentAppCurrency = Provider { currentAppCurrency.value },
            onHandleQuickAction = { handledAction, shouldDismiss ->
                handledQuickAction(handledAction, shouldDismiss)
            },
        )

    val bottomSheetNavigation: SlotNavigation<TokenReceiveConfig> = SlotNavigation()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TokenActionsUM?> =
        combine(
            params.data,
            getBalanceHidingSettingsUseCase.isBalanceHidden(),
            params.bottomAction,
        ) { cryptoCurrencyData, isBalanceHidden, bottomAction ->
            Triple(cryptoCurrencyData, isBalanceHidden, bottomAction)
        }
            .mapLatest { (cryptoCurrencyData, isBalanceHidden, bottomAction) ->
                uiBuilder.build(
                    cryptoCurrencyData = cryptoCurrencyData,
                    tokenActionsHandler = tokenActionsHandler,
                    appCurrency = currentAppCurrency.value,
                    isBalanceHidden = isBalanceHidden,
                    bottomAction = bottomAction,
                )
            }
            .flowOn(dispatchers.default)
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    private fun handledQuickAction(handledAction: TokenActionsHandler.HandledQuickAction, shouldDismiss: Boolean) =
        modelScope.launch {
            val isReceive = handledAction.action == TokenActionsBSContentUM.Action.Receive
            if (isReceive) {
                val tokenConfig = withContext(dispatchers.default) {
                    receiveAddressesFactory.create(
                        status = handledAction.cryptoCurrencyData.status,
                        userWalletId = handledAction.cryptoCurrencyData.userWallet.walletId,
                    )
                }
                if (tokenConfig != null) bottomSheetNavigation.activate(tokenConfig)
            }
            params.callbacks.onQuickActionClick(handledAction.action, shouldDismiss)
        }
}