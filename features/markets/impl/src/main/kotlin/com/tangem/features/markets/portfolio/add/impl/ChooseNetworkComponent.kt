package com.tangem.features.markets.portfolio.add.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.portfolio.add.impl.ui.ChooseNetworkContent
import com.tangem.features.markets.portfolio.add.impl.ui.state.ChooseNetworkUM
import com.tangem.features.markets.portfolio.impl.model.BlockchainRowUMConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toPersistentList

internal class ChooseNetworkComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableContentComponent {

    private val state by lazy {
        val converter = BlockchainRowUMConverter(
            alreadyAddedNetworks = params.alreadyAdded.mapTo(mutableSetOf()) { it.networkId },
        )
        val allAvailableNetworks = params.allAvailable.map { it to true }
        ChooseNetworkUM(
            networks = converter.convertList(allAvailableNetworks).toPersistentList(),
            onNetworkClick = onNetworkClick@{ row ->
                val network = params.allAvailable
                    .find { it.networkId == row.id }
                    ?: return@onNetworkClick
                params.callbacks.onNetworkSelected(network)
            },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        ChooseNetworkContent(state)
    }

    data class Params(
        val alreadyAdded: Set<TokenMarketInfo.Network>,
        val allAvailable: List<TokenMarketInfo.Network>,
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