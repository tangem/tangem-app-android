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
import kotlinx.coroutines.channels.BufferOverflow
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

    private val regionRefreshRequests = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val onrampRestrictedRegionName: StateFlow<String?> = combine(
        params.data.distinctUntilChangedBy { it.userWallet.walletId to it.status.currency.id },
        regionRefreshRequests,
    ) { data, _ -> data.userWallet }
        .mapLatest { onrampRegionRestrictionResolver.resolveRestrictedRegionName(it) }
        .flowOn(dispatchers.io)
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TokenActionsUM?> =
        combine(
            flow = params.data,
            flow2 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
            flow3 = params.bottomAction,
            flow4 = onrampRestrictedRegionName,
            transform = { cryptoCurrencyData, isBalanceHidden, bottomAction, restrictedRegionName ->
                TokenActionsBuildInput(
                    cryptoCurrencyData = cryptoCurrencyData,
                    isBalanceHidden = isBalanceHidden,
                    bottomAction = bottomAction,
                    onrampRestrictedRegionName = restrictedRegionName,
                )
            },
        )
            .mapLatest { input ->
                uiBuilder.build(
                    input = input,
                    tokenActionsHandler = tokenActionsHandler,
                    appCurrency = currentAppCurrency.value,
                )
            }
            .flowOn(dispatchers.default)
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    fun onStart() {
        regionRefreshRequests.tryEmit(Unit)
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
}