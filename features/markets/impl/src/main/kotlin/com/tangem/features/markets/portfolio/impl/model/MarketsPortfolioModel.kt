package com.tangem.features.markets.portfolio.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.GetAllWalletsCryptoCurrencyStatusesUseCase
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.features.markets.portfolio.impl.ui.state.QuickActionUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Stable
@ComponentScoped
internal class MarketsPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getAllWalletsCryptoCurrencyStatusesUseCase: GetAllWalletsCryptoCurrencyStatusesUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val state: StateFlow<MyPortfolioUM> get() = _state
    private val _state: MutableStateFlow<MyPortfolioUM> = MutableStateFlow(value = MyPortfolioUM.Loading)

    private val params = paramsContainer.require<MarketsPortfolioComponent.Params>()

    private val networks = MutableStateFlow<List<TokenMarketInfo.Network>>(value = emptyList())

    init {
        combine(
            flow = getAllWalletsCryptoCurrencyStatusesUseCase(params.tokenId),
            flow2 = networks,
        ) { map, networks ->
            val supportedTokens = map.flatMap { entry ->
                entry.value
                    .filter { either ->
                        either.isRight { status -> // TODO
                            networks.any { network ->
                                network.networkId == status.currency.network.id.value
                            }
                        }
                    }
                    .mapNotNull {
                        it.getOrNull()?.let { entry.key to it } // TODO
                    }
            }

            if (supportedTokens.isEmpty()) {
                MyPortfolioUM.AddFirstToken(onAddClick = ::onAddClick)
            } else {
                val tokens = supportedTokens.map { (userWallet, status) ->
                    TokenItemState.Loading(
                        id = params.tokenId,
                        iconState = CryptoCurrencyToIconStateConverter().convert(status.currency),
                        titleState = TokenItemState.TitleState.Content(text = userWallet.name),
                        subtitleState = TokenItemState.SubtitleState.TextContent(
                            value = status.currency.name,
                        ),
                    )
                }

                MyPortfolioUM.Tokens(
                    tokens = tokens.map {
                        PortfolioTokenUM(
                            tokenItemState = it,
                            isBalanceHidden = false,
                            isQuickActionsShown = false,
                            onQuickActionClick = {
                                when (it) {
                                    QuickActionUM.Buy -> TODO()
                                    QuickActionUM.Exchange -> TODO()
                                    QuickActionUM.Receive -> TODO()
                                }
                            },
                        )
                    }
                        .toImmutableList(),
                    buttonState = MyPortfolioUM.Tokens.AddButtonState.Available,
                    onAddClick = ::onAddClick,
                )
            }
        }
            .onEach { _state.value = it }
            .launchIn(modelScope)
    }

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        this.networks.value = networks
    }

    fun setNoNetworksAvailable() {
        _state.update { MyPortfolioUM.Unavailable }
    }

    private fun onAddClick() {
        // TODO
    }
}