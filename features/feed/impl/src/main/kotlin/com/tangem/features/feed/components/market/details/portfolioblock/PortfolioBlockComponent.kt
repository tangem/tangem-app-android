package com.tangem.features.feed.components.market.details.portfolioblock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.feed.components.market.details.portfolioblock.model.PortfolioBlockModel
import com.tangem.features.feed.components.market.details.portfolioblock.ui.PortfolioBlock
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.serialization.Serializable

@Stable
internal class PortfolioBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
    @Assisted private val parentRouter: PortfolioBlockParentClickIntents?,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: PortfolioBlockModel = getOrCreateModel(
        PortfolioBlockModelParams(
            token = params.token,
            parentRouter = parentRouter,
        ),
    )

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        model.setTokenNetworks(networks)
    }

    fun setNoNetworksAvailable() {
        model.setNoNetworksAvailable()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        PortfolioBlock(modifier = modifier, state = state)
    }

    @Serializable
    data class Params(val token: TokenMarketParams)

    @AssistedFactory
    interface Factory {
        fun create(
            context: AppComponentContext,
            params: Params,
            parentRouter: PortfolioBlockParentClickIntents?,
        ): PortfolioBlockComponent
    }

    data class PortfolioBlockModelParams(
        val token: TokenMarketParams,
        val parentRouter: PortfolioBlockParentClickIntents?,
    )
}