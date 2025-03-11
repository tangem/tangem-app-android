package com.tangem.feature.tester.presentation.providers.viewmodel

import androidx.core.util.PatternsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.providers.BlockchainProvidersTypesManager
import com.tangem.blockchainsdk.providers.MutableBlockchainProvidersTypesManager
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.providers.entity.BlockchainProvidersUM
import com.tangem.feature.tester.presentation.providers.entity.BlockchainProvidersUM.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class BlockchainProvidersViewModel @Inject constructor(
    blockchainProvidersTypesManager: BlockchainProvidersTypesManager,
) : ViewModel() {

    val state: StateFlow<BlockchainProvidersUM> get() = _state.asStateFlow()

    private val _state = MutableStateFlow(
        value = BlockchainProvidersUM(
            topBar = TopBarWithRefreshUM(
                titleResId = R.string.blockchain_providers,
                onBackClick = {},
                refreshButton = TopBarWithRefreshUM.RefreshButton(isVisible = false, onRefreshClick = ::onRefreshClick),
            ),
            searchBar = SearchBarUM(
                placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                query = "",
                onQueryChange = {
                    searchQuery.value = it.trim()
                },
                isActive = false,
                onActiveChange = {},
            ),
            blockchainProviders = persistentListOf(),
            onRestartAppClick = {},
        ),
    )

    private val searchQuery = MutableStateFlow(value = "")

    private val manager = blockchainProvidersTypesManager as MutableBlockchainProvidersTypesManager

    init {
        subscribeOnBlockchainProviderTypesUpdates()
    }

    fun setupNavigation(router: InnerTesterRouter, appFinisher: AppFinisher) {
        _state.update { state ->
            state.copy(
                topBar = state.topBar.copy(onBackClick = router::back),
                onRestartAppClick = appFinisher::restart,
            )
        }
    }

    private fun subscribeOnBlockchainProviderTypesUpdates() {
        combine(
            flow = manager.get(),
            flow2 = searchQuery,
            transform = { blockchainProviderTypes, query -> blockchainProviderTypes to query },
        )
            .onEach { (blockchainProviderTypes, query) ->
                val isChanged = !manager.isMatchWithMerged()

                _state.update { state ->
                    state.copy(
                        topBar = state.topBar.copy(
                            refreshButton = state.topBar.refreshButton.copy(isVisible = isChanged),
                        ),
                        searchBar = state.searchBar.copy(query = query),
                        blockchainProviders = blockchainProviderTypes
                            .filter {
                                it.key.fullName.contains(other = query, ignoreCase = true) ||
                                    it.key.currency.contains(other = query, ignoreCase = true)
                            }
                            .toProvidersUM(),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun Map<Blockchain, List<ProviderType>>.toProvidersUM(): ImmutableList<ProvidersUM> {
        return map {
            val (blockchain, providers) = it

            ProvidersUM(
                blockchainId = blockchain.id,
                blockchainName = blockchain.fullName,
                blockchainSymbol = blockchain.currency,
                providers = providers.map(BlockchainProvidersUM::ProviderUM).toImmutableList(),
                isExpanded = false,
                onDrop = { prev, current -> onDrop(blockchain.id, prev, current) },
                addPublicProviderDialog = AddPublicProviderDialogUM(
                    hasError = false,
                    onValueChange = { onPublicProviderUrlChange(id = blockchain.id, url = it) },
                    onSaveClick = { onAddProviderClick(id = blockchain.id, url = it) },
                ),
            )
        }
            .sortedBy(ProvidersUM::blockchainName)
            .toImmutableList()
    }

    private fun onRefreshClick() {
        viewModelScope.launch {
            manager.recoverInitialState()
        }
    }

    private fun onDrop(id: String, prev: Int, current: Int) {
        val providers = _state.value
            .blockchainProviders.first { it.blockchainId == id }
            .providers.changeItemOrder(prev, current)

        viewModelScope.launch {
            _state.update {
                it.copyProvidersUM(blockchainId = id) {
                    copy(providers = providers.toImmutableList())
                }
            }

            manager.update(
                blockchain = Blockchain.fromId(id),
                providers = providers.map(ProviderUM::type),
            )
        }
    }

    private fun ImmutableList<ProviderUM>.changeItemOrder(prev: Int, current: Int): ImmutableList<ProviderUM> {
        return when {
            prev > current && prev - current == 1 -> moveUpOneElement(prev, current)
            current > prev && current - prev == 1 -> moveDownOneElement(prev, current)
            else -> replaceElements(prev, current)
        }
            .toImmutableList()
    }

    // Before: [a, b, c, d, e]; previous = 2 (c), current = 1 (b)
    // After: [a, c, b, d, e]
    // Calculation:
    // 1) [0..current) = [0..1) = [a]
    // 2) [list[prev], list[current]] = [list[2], list[1]] = [c, b]
    // 3) [(prev+1)..list.size) = [3..5) = [d, e]
    // 4) [a] + [c, b] + [d, e] = [a, c, b, d, e]
    private fun List<ProviderUM>.moveUpOneElement(prev: Int, current: Int): List<ProviderUM> {
        return subList(fromIndex = 0, toIndex = current) +
            listOf(this[prev], this[current]) +
            subList(fromIndex = prev + 1, toIndex = this.size)
    }

    // Before: [a, b, c, d, e]; previous = 2 (c), current = 3 (d)
    // After: [a, b, d, c, e]
    // Calculation:
    // 1) [0..prev) = [0..2) = [a, b]
    // 2) [list[current], list[prev]] = [list[3], list[2]] = [d, c]
    // 3) [(current+1)..list.size) = [4..5) = [e]
    // 4) [a, b] + [d, c] + [e] = [a, b, d, c, e]
    private fun List<ProviderUM>.moveDownOneElement(prev: Int, current: Int): List<ProviderUM> {
        return subList(fromIndex = 0, toIndex = prev) +
            listOf(this[current], this[prev]) +
            subList(fromIndex = current + 1, toIndex = size)
    }

    // Before: [a, b, c, d, e]; previous = 0 (a), current = 4 (e)
    // After: [e, b, c, d, a]
    private fun List<ProviderUM>.replaceElements(prev: Int, current: Int): List<ProviderUM> {
        val mutableList = toMutableList()

        mutableList[prev] = this[current]
        mutableList[current] = this[prev]

        return mutableList
    }

    private fun onPublicProviderUrlChange(id: String, url: String) {
        _state.update {
            it.copyProvidersUM(blockchainId = id) {
                copy(
                    addPublicProviderDialog = addPublicProviderDialog.copy(
                        hasError = !PatternsCompat.WEB_URL.matcher(url).matches(),
                    ),
                )
            }
        }
    }

    private fun onAddProviderClick(id: String, url: String) {
        val providers = _state.value
            .blockchainProviders.first { it.blockchainId == id }
            .providers

        viewModelScope.launch {
            manager.update(
                blockchain = Blockchain.fromId(id),
                providers = listOf(ProviderType.Public(url = url)) + providers.map(ProviderUM::type),
            )
        }
    }

    private fun BlockchainProvidersUM.copyProvidersUM(
        blockchainId: String,
        update: ProvidersUM.() -> ProvidersUM,
    ): BlockchainProvidersUM {
        return copy(
            blockchainProviders = _state.value.blockchainProviders.map {
                if (it.blockchainId == blockchainId) {
                    it.update()
                } else {
                    it
                }
            }
                .toImmutableList(),
        )
    }
}