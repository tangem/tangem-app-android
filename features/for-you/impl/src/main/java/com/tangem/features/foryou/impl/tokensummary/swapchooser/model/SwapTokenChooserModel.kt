package com.tangem.features.foryou.impl.tokensummary.swapchooser.model

import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentConverter
import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.foryou.impl.tokensummary.swapchooser.SwapTokenChooserComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class SwapTokenChooserModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
) : Model() {

    private val params = paramsContainer.require<SwapTokenChooserComponent.Params>()

    val content: StateFlow<TokenSelectorContentUM?> = combine(
        flow = params.entries,
        flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
        flow3 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
        transform = ::buildContent,
    )
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = null)

    init {
        params.entries
            .filter(List<TokenSelectorEntry>::isEmpty)
            .onEach { params.callbacks.onDismiss() }
            .launchIn(modelScope)
    }

    fun onDismiss() = params.callbacks.onDismiss()

    private fun buildContent(
        entries: List<TokenSelectorEntry>,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
    ): TokenSelectorContentUM? {
        if (entries.isEmpty()) return null

        return TokenSelectorContentConverter(
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            resolveWalletDeviceIcon = { walletIconUMConverter.convert(getWalletIconUseCase(it)) },
            onEntryClick = { entry ->
                params.callbacks.onTokenSelected(entry.wallet.walletId, entry.currencyStatus)
            },
        ).convert(entries)
    }
}