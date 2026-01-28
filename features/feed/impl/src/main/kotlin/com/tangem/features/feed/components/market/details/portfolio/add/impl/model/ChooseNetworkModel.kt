package com.tangem.features.feed.components.market.details.portfolio.add.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.feed.components.market.details.portfolio.add.impl.ChooseNetworkComponent
import com.tangem.features.feed.components.market.details.portfolio.add.impl.ui.state.ChooseNetworkUM
import com.tangem.features.feed.components.market.details.portfolio.impl.model.BlockchainRowUMConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class ChooseNetworkModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val checkCurrencyUnsupportedDelegate: CheckCurrencyUnsupportedDelegate,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<ChooseNetworkComponent.Params>()

    val uiState: StateFlow<ChooseNetworkUM> = MutableStateFlow(buildUI())

    private fun buildUI(): ChooseNetworkUM {
        val allAvailable = params.selectedPortfolio.account.availableNetworks
        val alreadyAdded = allAvailable
            .subtract(params.selectedPortfolio.account.availableToAddNetworks)
        val converter = BlockchainRowUMConverter(
            alreadyAddedNetworks = alreadyAdded.mapTo(mutableSetOf()) { it.networkId },
        )
        val allAvailableNetworks = allAvailable.map { it to true }
        return ChooseNetworkUM(
            networks = converter.convertList(allAvailableNetworks).toPersistentList(),
            onNetworkClick = onNetworkClick@{ row ->
                val network = allAvailable
                    .find { it.networkId == row.id }
                    ?: return@onNetworkClick
                checkNetwork(row, network)
            },
        )
    }

    private fun checkNetwork(row: BlockchainRowUM, network: TokenMarketInfo.Network) = modelScope.launch {
        val selectedWalletId = params.selectedPortfolio.userWallet.walletId
        val unsupportedState = checkCurrencyUnsupportedDelegate.checkCurrencyUnsupportedState(
            userWalletId = selectedWalletId,
            rawNetworkId = row.id,
            isMainNetwork = row.isMainNetwork,
        )
        if (unsupportedState == null) {
            params.callbacks.onNetworkSelected(network)
        }
    }
}