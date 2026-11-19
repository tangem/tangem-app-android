package com.tangem.features.collectibles.impl.inventory.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.nftdetails.CollectiblesNftDetailsComponent
import com.tangem.features.collectibles.impl.inventory.model.converter.CollectiblesInventoryContentConverter
import com.tangem.features.collectibles.impl.inventory.model.converter.CollectiblesInventoryConverter
import com.tangem.features.collectibles.impl.inventory.model.converter.CollectiblesInventoryTabsConverter
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.combine6
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CollectiblesInventoryModel @Inject constructor(
    private val inventorySource: CollectiblesInventoryMockSource,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val converter = CollectiblesInventoryConverter(
        tabsConverter = CollectiblesInventoryTabsConverter(onTabClick = ::onTabClick),
        contentConverter = CollectiblesInventoryContentConverter(
            onCardClick = ::onCardClick,
            onReloadClick = ::onReloadClick,
        ),
        onSearchQueryChange = ::onSearchQueryChange,
        onSearchActiveChange = ::onSearchActiveChange,
        onSearchCloseClick = ::onSearchCloseClick,
    )

    private val selectedTab = MutableStateFlow(CollectiblesInventoryTab.Buyback)
    private val refreshTrigger = MutableStateFlow<CollectiblesInventoryRefreshTrigger?>(null)
    private val search = MutableStateFlow(CollectiblesInventorySearch(query = "", isActive = false))
    private var refreshJob: Job? = null

    val uiState: StateFlow<CollectiblesInventoryUM> = combine6(
        flow1 = selectedTab,
        flow2 = inventorySource.observe(CollectiblesInventoryTab.Buyback).startWithLoading(),
        flow3 = inventorySource.observe(CollectiblesInventoryTab.Collection).startWithLoading(),
        flow4 = refreshTrigger,
        flow5 = search,
        flow6 = observeSearchCards(),
    ) { tab, buybackCards, collectionCards, refreshTrigger, search, searchCards ->
        converter.convert(
            selectedTab = tab,
            cardsByTab = mapOf(
                CollectiblesInventoryTab.Buyback to buybackCards,
                CollectiblesInventoryTab.Collection to collectionCards,
            ),
            refreshTrigger = refreshTrigger,
            search = search,
            searchCards = searchCards,
        )
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = converter.convert(
            selectedTab = selectedTab.value,
            cardsByTab = emptyMap(),
            refreshTrigger = null,
            search = search.value,
            searchCards = emptyList(),
        ),
    )

    init {
        refreshJob = modelScope.launch { inventorySource.refresh() }
    }

    fun onRefresh() {
        refresh(trigger = CollectiblesInventoryRefreshTrigger.PullToRefresh)
    }

    fun onSearchCloseClick() {
        search.value = CollectiblesInventorySearch(query = "", isActive = false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSearchCards(): Flow<List<CollectiblesInventoryMockSource.Card>> = search
        .flatMapLatest { search ->
            if (search.isSearchMode) {
                inventorySource.search(search.query.trim())
            } else {
                flowOf(emptyList())
            }
        }

    private fun onReloadClick() {
        refresh(trigger = CollectiblesInventoryRefreshTrigger.Reload)
    }

    private fun refresh(trigger: CollectiblesInventoryRefreshTrigger) {
        if (refreshJob?.isActive == true) return

        refreshJob = modelScope.launch {
            refreshTrigger.value = trigger
            try {
                inventorySource.refresh()
            } finally {
                refreshTrigger.value = null
            }
        }
    }

    private fun onTabClick(tab: CollectiblesInventoryTab) {
        selectedTab.value = tab
    }

    private fun onSearchQueryChange(query: String) {
        search.update { it.copy(query = query) }
    }

    private fun onSearchActiveChange(isActive: Boolean) {
        search.update { it.copy(isActive = isActive) }
    }

    private fun onCardClick(cardId: String) {
        router.push(
            CollectiblesRoute.NftDetails(id = cardId, ownership = CollectiblesNftDetailsComponent.Ownership.Owned),
        )
    }

    /** `null` until the first result arrives: the tab is still loading. */
    private fun <T> Flow<T>.startWithLoading(): Flow<T?> = map<T, T?> { it }.onStart { emit(null) }
}