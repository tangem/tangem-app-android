package com.tangem.features.markets.portfolio.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.markets.portfolio.impl.model.MarketsPortfolioModel
import com.tangem.features.markets.portfolio.impl.ui.MyPortfolio
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultMarketsPortfolioComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: MarketsPortfolioComponent.Params,
) : AppComponentContext by context, MarketsPortfolioComponent {

    private val model: MarketsPortfolioModel = getOrCreateModel(params)

    override fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) = model.setTokenNetworks(networks)
    override fun setNoNetworksAvailable() = model.setNoNetworksAvailable()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        MyPortfolio(modifier = modifier, state = state)
    }

    @AssistedFactory
    interface Factory : MarketsPortfolioComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MarketsPortfolioComponent.Params,
        ): DefaultMarketsPortfolioComponent
    }
}