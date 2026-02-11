package com.tangem.features.feed.components.market.details.portfolio.add.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.feed.components.market.details.portfolio.add.SelectedPortfolio
import com.tangem.features.feed.components.market.details.portfolio.add.impl.model.ChooseNetworkModel
import com.tangem.features.feed.components.market.details.portfolio.add.impl.ui.ChooseNetworkContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class ChooseNetworkComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableContentComponent {

    private val model: ChooseNetworkModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        ChooseNetworkContent(state)
    }

    data class Params(
        val selectedPortfolio: SelectedPortfolio,
        val callbacks: Callbacks,
    )

    interface Callbacks {
        fun onNetworkSelected(network: TokenMarketInfo.Network)
    }

    @AssistedFactory
    interface Factory : ComponentFactory<Params, ChooseNetworkComponent> {
        override fun create(context: AppComponentContext, params: Params): ChooseNetworkComponent
    }
}