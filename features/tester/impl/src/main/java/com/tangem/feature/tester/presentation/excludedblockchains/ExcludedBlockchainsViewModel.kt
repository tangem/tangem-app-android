package com.tangem.feature.tester.presentation.excludedblockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.configtoggle.blockchain.MutableExcludedBlockchainsManager
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.excludedblockchains.state.BlockchainUM
import com.tangem.feature.tester.presentation.excludedblockchains.state.ExcludedBlockchainsScreenUM
import com.tangem.feature.tester.presentation.excludedblockchains.state.mapper.toUiModels
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.utils.version.AppVersionProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ExcludedBlockchainsViewModel @Inject constructor(
    private val appVersionProvider: AppVersionProvider,
    excludedBlockchainsManager: MutableExcludedBlockchainsManager?,
) : ViewModel() {

    private val excludedBlockchainsManager: MutableExcludedBlockchainsManager =
        requireNotNull(excludedBlockchainsManager) {
            "Mutable excluded blockchains manager can't be null when tester actions is available"
        }

    val state: MutableStateFlow<ExcludedBlockchainsScreenUM> = MutableStateFlow(
        value = getInitialState(),
    )

    init {
        observeQueryUpdates()
    }

    fun setupNavigation(router: InnerTesterRouter, appFinisher: AppFinisher) {
        state.update { state ->
            state.copy(
                popBack = router::back,
                onRestartClick = appFinisher::restart,
            )
        }
    }

    private fun getInitialState(): ExcludedBlockchainsScreenUM = ExcludedBlockchainsScreenUM(
        popBack = {},
        search = getInitialSearchBar(),
        blockchains = getBlockchains(),
        showRecoverWarning = !excludedBlockchainsManager.isMatchLocalConfig(),
        appVersion = appVersionProvider.versionName,
        onRestartClick = {},
        onRecoverClick = ::recoverLocalConfig,
    )

    @OptIn(FlowPreview::class)
    private fun observeQueryUpdates() {
        state
            .map { it.search.query }
            .distinctUntilChanged()
            .drop(count = 1) // Skip initial value
            .sample(periodMillis = 1_000)
            .onEach(::filterBlockchains)
            .launchIn(viewModelScope)
    }

    private fun recoverLocalConfig() = viewModelScope.launch {
        excludedBlockchainsManager.recoverLocalConfig()

        state.update { state ->
            state.copy(
                search = getInitialSearchBar(),
                blockchains = getBlockchains(),
            )
        }
    }

    private fun getBlockchains(): PersistentList<BlockchainUM> {
        val excludedBlockchains = excludedBlockchainsManager.excludedBlockchainsIds

        return Blockchain.entries.toUiModels(
            excludedBlockchainsIds = excludedBlockchains,
            onExcludedStateChange = ::excludeBlockchain,
        ).toPersistentList()
    }

    private fun getInitialSearchBar(): SearchBarUM = SearchBarUM(
        placeholderText = resourceReference(R.string.excluded_blockchains_search_placeholder),
        query = "",
        isActive = false,
        onQueryChange = ::updateQuery,
        onActiveChange = ::updateSearchBarState,
    )

    private fun excludeBlockchain(blockchain: Blockchain, isExcluded: Boolean) = viewModelScope.launch {
        excludedBlockchainsManager.excludeBlockchain(blockchain.id, isExcluded)

        state.update { state ->
            state.copy(
                blockchains = state.blockchains.mutate { mutableBlockchains ->
                    val index = mutableBlockchains.indexOfFirst { it.id == blockchain.id }
                    if (index == -1) return@mutate

                    mutableBlockchains[index] = mutableBlockchains[index].copy(isExcluded = isExcluded)
                },
                showRecoverWarning = !excludedBlockchainsManager.isMatchLocalConfig(),
            )
        }
    }

    private fun filterBlockchains(query: String) {
        val filteredBlockchains = if (query.isBlank()) {
            getBlockchains()
        } else {
            getBlockchains().filter { blockchain ->
                blockchain.name.contains(query, ignoreCase = true) ||
                    blockchain.symbol.contains(query, ignoreCase = true)
            }.toPersistentList()
        }

        state.update { state ->
            state.copy(
                blockchains = filteredBlockchains,
            )
        }
    }

    private fun updateQuery(query: String) {
        state.update { state ->
            state.copy(
                search = state.search.copy(query = query),
            )
        }
    }

    private fun updateSearchBarState(isActive: Boolean) {
        state.update { state ->
            state.copy(
                search = state.search.copy(isActive = isActive),
            )
        }
    }
}