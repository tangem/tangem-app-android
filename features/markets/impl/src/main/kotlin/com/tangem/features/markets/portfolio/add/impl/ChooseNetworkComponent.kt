package com.tangem.features.markets.portfolio.add.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.portfolio.add.api.SelectedPortfolio
import com.tangem.features.markets.portfolio.add.impl.ui.ChooseNetworkContent
import com.tangem.features.markets.portfolio.add.impl.ui.state.ChooseNetworkUM
import com.tangem.features.markets.portfolio.impl.model.BlockchainRowUMConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*

internal class ChooseNetworkComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableContentComponent {

    private val uiState: StateFlow<ChooseNetworkUM?> = params.data
        .map { portfolio -> buildUM(portfolio) }
        .flowOn(dispatchers.default)
        .stateIn(componentScope, started = SharingStarted.Eagerly, null)

    @Composable
    override fun Content(modifier: Modifier) {
        val state = uiState.collectAsStateWithLifecycle()
        val um = state.value ?: return
        ChooseNetworkContent(um)
    }

    private fun buildUM(portfolio: SelectedPortfolio): ChooseNetworkUM {
        val allAvailable = portfolio.account.availableNetworks
        val alreadyAdded = portfolio.account.addedMarketNetworks
            .mapTo(mutableSetOf()) { it.networkId }
        val converter = BlockchainRowUMConverter(alreadyAddedNetworks = alreadyAdded)
        val allAvailableNetworks = allAvailable.map { it to true }
        val onNetworkClick: (BlockchainRowUM) -> Unit = onNetworkClick@{ row ->
            val network = allAvailable
                .find { it.networkId == row.id }
                ?: return@onNetworkClick
            params.callbacks.onNetworkSelected(network)
        }
        return ChooseNetworkUM(
            networks = converter.convertList(allAvailableNetworks).toPersistentList(),
            onNetworkClick = onNetworkClick,
        )
    }

    data class Params(
        val data: Flow<SelectedPortfolio>,
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