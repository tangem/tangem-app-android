package com.tangem.features.markets.portfolio.impl.model

import android.util.Log
import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.GetAllWalletsCryptoCurrencyStatusesUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@Stable
@ComponentScoped
internal class MarketsPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getAllWalletsCryptoCurrencyStatusesUseCase: GetAllWalletsCryptoCurrencyStatusesUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val state: StateFlow<MyPortfolioUM> get() = _state
    private val _state: MutableStateFlow<MyPortfolioUM> = MutableStateFlow(value = MyPortfolioUM.Loading)

    private val params = paramsContainer.require<MarketsPortfolioComponent.Params>()

    private val networks = MutableStateFlow<List<TokenMarketInfo.Network>?>(value = null)

    init {
        val mapper = MyPortfolioUMMapper(::onAddClick, ::onTokenItemClick, ::onTokenItemClick) // TODO

        combine(
            flow = getAllWalletsCryptoCurrencyStatusesUseCase(params.tokenId),
            flow2 = networks,
            flow3 = getSelectedAppCurrencyUseCase(),
            flow4 = getBalanceHidingSettingsUseCase(),
        ) { map, networks, maybeAppCurrency, isBalanceHidden ->
            val appCurrency = maybeAppCurrency.getOrElse { e ->
                Timber.e("Failed to load app currency: $e")
                AppCurrency.Default
            }

            mapper.map(map, networks, appCurrency, isBalanceHidden.isBalanceHidden)
        }
            .onEach {
                Log.i("MarketsPortfolioModel", "onEach: $it")
                _state.value = it
            }
            .launchIn(modelScope)
    }

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        Log.i("MarketsPortfolioModel", "setTokenNetworks: $networks")
        this.networks.value = networks
    }

    fun setNoNetworksAvailable() {
        Log.i("MarketsPortfolioModel", "setNoNetworksAvailable")
        this.networks.value = emptyList()
    }

    private fun onAddClick() {
        // TODO
    }

    private fun onTokenItemClick(token: CryptoCurrencyStatus) {
        _state.update {
            (_state.value as? MyPortfolioUM.Tokens)?.let { state ->
                state.copy(
                    tokens = state.tokens.map { tokenUM ->
                        if (tokenUM.tokenItemState.id == token.currency.id.value) {
                            tokenUM.copy(isQuickActionsShown = !tokenUM.isQuickActionsShown)
                        } else {
                            tokenUM
                        }
                    }
                        .toImmutableList(),
                )
            } ?: it
        }
    }
}