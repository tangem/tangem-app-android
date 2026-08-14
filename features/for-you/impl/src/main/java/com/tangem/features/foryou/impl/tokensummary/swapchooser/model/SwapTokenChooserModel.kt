package com.tangem.features.foryou.impl.tokensummary.swapchooser.model

import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentConverter
import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.foryou.impl.tokensummary.model.SwapHolding
import com.tangem.features.foryou.impl.tokensummary.swapchooser.SwapTokenChooserComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class SwapTokenChooserModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
) : Model() {

    private val params = paramsContainer.require<SwapTokenChooserComponent.Params>()

    val content: StateFlow<TokenSelectorContentUM?> = combine(
        flow = params.holdings,
        flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
        flow3 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
        flow4 = isAccountsModeEnabledUseCase(),
        transform = ::buildContent,
    )
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = null)

    init {
        params.holdings
            .filter(List<SwapHolding>::isEmpty)
            .onEach { params.callbacks.onDismiss() }
            .launchIn(modelScope)
    }

    fun onDismiss() = params.callbacks.onDismiss()

    private fun buildContent(
        holdings: List<SwapHolding>,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
        isAccountsModeEnabled: Boolean,
    ): TokenSelectorContentUM? {
        if (holdings.isEmpty()) return null

        return TokenSelectorContentConverter(
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            isAccountsModeEnabled = isAccountsModeEnabled,
            resolveWalletDeviceIcon = { walletIconUMConverter.convert(getWalletIconUseCase(it)) },
            onEntryClick = { entry ->
                holdings.firstOrNull { it.entry == entry }?.let(params.callbacks::onHoldingSelected)
            },
        ).convert(holdings.map(SwapHolding::entry))
    }
}