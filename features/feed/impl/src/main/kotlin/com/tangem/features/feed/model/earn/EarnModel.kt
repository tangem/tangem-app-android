package com.tangem.features.feed.model.earn

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.feed.components.earn.DefaultEarnComponent
import com.tangem.features.feed.ui.earn.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.ui.earn.state.EarnFilterByNetworkBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterByTypeBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.earn.state.EarnUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class EarnModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<DefaultEarnComponent.Params>()

    // TODO [REDACTED_TASK_KEY] remove mocks
    private val mockNetworks: ImmutableList<EarnFilterNetworkUM> = persistentListOf(
        EarnFilterNetworkUM.AllNetworks(
            text = resourceReference(R.string.earn_filter_all_networks),
            isSelected = false,
        ),
        EarnFilterNetworkUM.MyNetworks(
            text = resourceReference(R.string.earn_filter_my_networks),
            isSelected = false,
        ),
        EarnFilterNetworkUM.Network(
            id = "ethereum",
            text = stringReference("Ethereum"),
            symbol = stringReference("ETH"),
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/ethereum.png",
            isSelected = false,
        ),
        EarnFilterNetworkUM.Network(
            id = "polygon",
            text = stringReference("Polygon"),
            symbol = stringReference("MATIC"),
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/matic-network.png",
            isSelected = false,
        ),
        EarnFilterNetworkUM.Network(
            id = "base",
            text = stringReference("Base"),
            symbol = stringReference("BASE"),
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/base.png",
            isSelected = false,
        ),
        EarnFilterNetworkUM.Network(
            id = "avalanche",
            text = stringReference("Avalance"),
            symbol = stringReference("AVAX"),
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/avalanche-2.png",
            isSelected = false,
        ),
        EarnFilterNetworkUM.Network(
            id = "cardano",
            text = stringReference("Cardano"),
            symbol = stringReference("ADA"),
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/cardano.png",
            isSelected = false,
        ),
        EarnFilterNetworkUM.Network(
            id = "bsc",
            text = stringReference("Bitcoin Smart Chain"),
            symbol = stringReference("BSC"),
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/binancecoin.png",
            isSelected = false,
        ),
        EarnFilterNetworkUM.Network(
            id = "bitcoin",
            text = stringReference("Bitcoin"),
            symbol = stringReference("BTC"),
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/bitcoin.png",
            isSelected = false,
        ),
        EarnFilterNetworkUM.Network(
            id = "solana",
            text = stringReference("Solana"),
            symbol = stringReference("SOL"),
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/solana.png",
            isSelected = false,
        ),
        EarnFilterNetworkUM.Network(
            id = "ton",
            text = stringReference("Ton"),
            symbol = stringReference("TON"),
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/the-open-network.png",
            isSelected = false,
        ),
    )

    val state: StateFlow<EarnUM>
        field = MutableStateFlow(createInitialState())

    private fun createInitialState(): EarnUM {
        val defaultNetworkFilter = mockNetworks.first()
        val defaultTypeFilter = EarnFilterTypeUM.All

        return EarnUM(
            mostlyUsed = EarnListUM.Loading,
            bestOpportunities = EarnBestOpportunitiesUM.Loading,
            selectedTypeFilter = defaultTypeFilter,
            selectedNetworkFilter = defaultNetworkFilter,
            filterByTypeBottomSheet = createTypeFilterBottomSheetConfig(defaultTypeFilter),
            filterByNetworkBottomSheet = createNetworkFilterBottomSheetConfig(defaultNetworkFilter),
            onBackClick = params.onBackClick,
            onNetworkFilterClick = ::onNetworkFilterClick,
            onTypeFilterClick = ::onTypeFilterClick,
        )
    }

    private fun createTypeFilterBottomSheetConfig(
        selectedOption: EarnFilterTypeUM,
        isShown: Boolean = false,
    ): TangemBottomSheetConfig {
        return TangemBottomSheetConfig(
            isShown = isShown,
            onDismissRequest = ::hideTypeFilterBottomSheet,
            content = EarnFilterByTypeBottomSheetContentUM(
                selectedOption = selectedOption,
                onOptionClick = ::onTypeFilterOptionClick,
            ),
        )
    }

    private fun createNetworkFilterBottomSheetConfig(
        selectedNetwork: EarnFilterNetworkUM,
        isShown: Boolean = false,
    ): TangemBottomSheetConfig {
        val updatedNetworks = mockNetworks.map { network ->
            when {
                network is EarnFilterNetworkUM.AllNetworks && selectedNetwork is EarnFilterNetworkUM.AllNetworks ->
                    network.copy(isSelected = true)
                network is EarnFilterNetworkUM.MyNetworks && selectedNetwork is EarnFilterNetworkUM.MyNetworks ->
                    network.copy(isSelected = true)
                network is EarnFilterNetworkUM.Network && selectedNetwork is EarnFilterNetworkUM.Network &&
                    network.id == selectedNetwork.id ->
                    network.copy(isSelected = true)
                else -> network
            }
        }

        return TangemBottomSheetConfig(
            isShown = isShown,
            onDismissRequest = ::hideNetworkFilterBottomSheet,
            content = EarnFilterByNetworkBottomSheetContentUM(
                selectedNetwork = selectedNetwork,
                networks = persistentListOf(*updatedNetworks.toTypedArray()),
                onOptionClick = ::onNetworkFilterOptionClick,
            ),
        )
    }

    private fun onTypeFilterClick() {
        state.update { currentState ->
            currentState.copy(
                filterByTypeBottomSheet = createTypeFilterBottomSheetConfig(
                    selectedOption = currentState.selectedTypeFilter,
                    isShown = true,
                ),
            )
        }
    }

    private fun onNetworkFilterClick() {
        state.update { currentState ->
            currentState.copy(
                filterByNetworkBottomSheet = createNetworkFilterBottomSheetConfig(
                    selectedNetwork = currentState.selectedNetworkFilter,
                    isShown = true,
                ),
            )
        }
    }

    private fun hideTypeFilterBottomSheet() {
        state.update { currentState ->
            currentState.copy(
                filterByTypeBottomSheet = currentState.filterByTypeBottomSheet.copy(isShown = false),
            )
        }
    }

    private fun hideNetworkFilterBottomSheet() {
        state.update { currentState ->
            currentState.copy(
                filterByNetworkBottomSheet = currentState.filterByNetworkBottomSheet.copy(isShown = false),
            )
        }
    }

    private fun onTypeFilterOptionClick(type: EarnFilterTypeUM) {
        state.update { currentState ->
            currentState.copy(
                selectedTypeFilter = type,
                filterByTypeBottomSheet = createTypeFilterBottomSheetConfig(selectedOption = type),
            )
        }
    }

    private fun onNetworkFilterOptionClick(network: EarnFilterNetworkUM) {
        state.update { currentState ->
            currentState.copy(
                selectedNetworkFilter = network,
                filterByNetworkBottomSheet = createNetworkFilterBottomSheetConfig(selectedNetwork = network),
            )
        }
    }

    // TODO [REDACTED_TASK_KEY] remove
    @Suppress("UnusedPrivateMember")
    private fun onClearFiltersClick() {
        val defaultNetworkFilter = mockNetworks.first()
        val defaultTypeFilter = EarnFilterTypeUM.All
        state.update { currentState ->
            currentState.copy(
                selectedTypeFilter = defaultTypeFilter,
                selectedNetworkFilter = defaultNetworkFilter,
                filterByTypeBottomSheet = createTypeFilterBottomSheetConfig(selectedOption = defaultTypeFilter),
                filterByNetworkBottomSheet = createNetworkFilterBottomSheetConfig(
                    selectedNetwork = defaultNetworkFilter,
                ),
            )
        }
    }
}