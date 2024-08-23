package com.tangem.features.markets.portfolio.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.features.markets.portfolio.impl.ui.state.QuickActionUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ComponentScoped
internal class MarketsPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    // private val getWalletsUseCase: GetWalletsUseCase,
    // private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    // private val getTokenListUseCase: GetTokenListUseCase,
    // private val getAllWalletsCryptoCurrencyStatusesUseCase: GetAllWalletsCryptoCurrencyStatusesUseCase,
) : Model() {

    val state: StateFlow<MyPortfolioUM> get() = _state
    private val _state: MutableStateFlow<MyPortfolioUM> = MutableStateFlow(value = MyPortfolioUM.Loading)

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<MarketsPortfolioComponent.Params>()

    // private val tokens = MutableStateFlow<>init {
    //     getAllWalletsCryptoCurrencyStatusesUseCase(params.tokenId)
    //         .collectLatest {
    //         }
    // }

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        _state.update {
            if (networks.isEmpty()) {
                MyPortfolioUM.AddFirstToken(onAddClick = ::onAddClick)
            } else {
                MyPortfolioUM.Tokens(
                    tokens = networks.map {
                        PortfolioTokenUM(
                            tokenItemState = TokenItemState.Loading(
                                id = params.tokenId,
                                iconState = TODO(),
                                titleState = TokenItemState.TitleState.Content(
                                    text = "decore",
                                ),
                                // subtitleState = TokenItemState.SubtitleState.TextContent(
                                //     value = ,
                                // ),
                            ),
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
    }

    fun setNoNetworksAvailable() {
        _state.update { MyPortfolioUM.Unavailable }
    }

    private fun onAddClick() {
        // TODO
    }
}