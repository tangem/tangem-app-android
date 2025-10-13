package com.tangem.features.markets.portfolio.add.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent.AnalyticsParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal interface AddToPortfolioComponent : ComposableBottomSheetComponent {

    val state: StateFlow<State>
    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>)

    data class Params(
        val token: TokenMarketParams,
        val analyticsParams: AnalyticsParams?,
        val callback: Callback,
    )

    interface Callback {
        fun onDismiss()
    }

    sealed interface State {
        data object Init : State
        data class AvailableToAdd(
            val availableToAddData: Flow<AvailableToAddData>,
        ) : State

        data object NothingToAdd : State
    }

    interface Factory : ComponentFactory<Params, AddToPortfolioComponent>
}