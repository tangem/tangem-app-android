package com.tangem.feature.tester.presentation.providers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.providers.BlockchainProviderTypesStore
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.providers.entity.BlockchainProvidersUM
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
internal class BlockchainProvidersViewModel @Inject constructor(
    blockchainProviderTypesStore: BlockchainProviderTypesStore,
) : ViewModel() {

    val state: StateFlow<BlockchainProvidersUM> get() = _state.asStateFlow()

    private val _state = MutableStateFlow(
        value = BlockchainProvidersUM(onBackClick = {}, blockchainProviders = persistentListOf()),
    )

    init {
        blockchainProviderTypesStore.get()
            .onEach { blockchainProviderTypes ->
                _state.update { state ->
                    state.copy(blockchainProviders = blockchainProviderTypes.toProvidersUM())
                }
            }
            .launchIn(viewModelScope)
    }

    fun setupNavigation(router: InnerTesterRouter) {
        _state.update { state ->
            state.copy(onBackClick = router::back)
        }
    }

    private fun Map<Blockchain, List<ProviderType>>.toProvidersUM(): ImmutableList<BlockchainProvidersUM.ProvidersUM> {
        return map {
            val (blockchain, providers) = it

            BlockchainProvidersUM.ProvidersUM(
                blockchainId = blockchain.id,
                blockchainName = blockchain.fullName,
                blockchainSymbol = blockchain.currency,
                providers = providers.map { providerType ->
                    BlockchainProvidersUM.ProviderUM(
                        name = when (providerType) {
                            is ProviderType.Public -> providerType.url
                            else -> providerType::class.java.simpleName
                        },
                    )
                }
                    .toImmutableList(),
                isExpanded = false,
            )
        }
            .toImmutableList()
    }
}
