package com.tangem.features.commonfeatures.impl.tokenactions.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.common.ui.markets.action.TokenActionsContext
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
import kotlinx.coroutines.Job
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
    private val onrampRegionRestrictionResolver: OnrampRegionRestrictionResolver,
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
            coroutineScope = modelScope,
        )

    val bottomSheetNavigation: SlotNavigation<TokenReceiveConfig> = SlotNavigation()

    private val selectedToken = MutableStateFlow<SelectedToken?>(null)
    private var lookupJob: Job? = null
    private var hasStarted = false

    val uiState: StateFlow<TokenActionsUM?> =
        combine(
            selectedToken.filterNotNull(),
            getBalanceHidingSettingsUseCase.isBalanceHidden(),
            params.bottomAction,
        ) { token, isBalanceHidden, bottomAction ->
            uiBuilder.build(
                input = TokenActionsBuildInput(token.data, isBalanceHidden, bottomAction, token.region),
                tokenActionsHandler = tokenActionsHandler,
                appCurrency = currentAppCurrency.value,
                canBuy = {
                    val current = selectedToken.value
                    current?.region == OnrampRegionState.Allowed && current.matches(token.data)
                },
            )
        }
            .flowOn(dispatchers.default)
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    fun onStart() {
        if (hasStarted) {
            selectedToken.value?.let { refreshRegion(it.data) }
        } else {
            hasStarted = true
            params.data.flowOn(dispatchers.default).onEach(::onTokenChanged).launchIn(modelScope)
        }
    }

    fun dismissReceive() {
        bottomSheetNavigation.dismiss { dismissed ->
            if (dismissed) selectedToken.value?.let { refreshRegion(it.data) }
        }
    }

    private fun onTokenChanged(data: CryptoCurrencyData) {
        val previous = selectedToken.value
        if (previous != null && previous.matches(data)) {
            selectedToken.value = previous.copy(data = data)
        } else {
            refreshRegion(data)
        }
    }

    private fun refreshRegion(data: CryptoCurrencyData) {
        lookupJob?.cancel()
        if (params.context != TokenActionsContext.AddFunds || !onrampRegionRestrictionResolver.isEnabled) {
            selectedToken.value = SelectedToken(data, OnrampRegionState.Allowed)
            return
        }

        selectedToken.value = SelectedToken(data, OnrampRegionState.Checking)
        lookupJob = modelScope.launch {
            val result = withContext(dispatchers.io) { onrampRegionRestrictionResolver.resolve(data.userWallet) }
            selectedToken.update { it?.copy(region = result) }
        }
    }

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

    private data class SelectedToken(val data: CryptoCurrencyData, val region: OnrampRegionState) {
        fun matches(other: CryptoCurrencyData): Boolean =
            data.userWallet.walletId == other.userWallet.walletId && data.status.currency.id == other.status.currency.id
    }
}