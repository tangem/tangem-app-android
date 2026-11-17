package com.tangem.features.collectibles.impl.inventory.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.collectibles.impl.inventory.model.converter.CollectiblesInventoryContentConverter
import com.tangem.features.collectibles.impl.inventory.model.converter.CollectiblesInventoryConverter
import com.tangem.features.collectibles.impl.inventory.model.converter.CollectiblesInventoryTabsConverter
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CollectiblesInventoryModel @Inject constructor(
    private val inventorySource: CollectiblesInventoryMockSource,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val converter = CollectiblesInventoryConverter(
        tabsConverter = CollectiblesInventoryTabsConverter(onTabClick = ::onTabClick),
        contentConverter = CollectiblesInventoryContentConverter(
            onCardClick = ::onCardClick,
            onReloadClick = ::onReloadClick,
        ),
    )

    private val selectedTab = MutableStateFlow(CollectiblesInventoryTab.Buyback)
    private val refreshTrigger = MutableStateFlow<CollectiblesInventoryRefreshTrigger?>(null)
    private var refreshJob: Job? = null

    val uiState: StateFlow<CollectiblesInventoryUM> = combine(
        flow = selectedTab,
        flow2 = inventorySource.observe(CollectiblesInventoryTab.Buyback).startWithLoading(),
        flow3 = inventorySource.observe(CollectiblesInventoryTab.Collection).startWithLoading(),
        flow4 = refreshTrigger,
    ) { tab, buybackCards, collectionCards, refreshTrigger ->
        converter.convert(
            selectedTab = tab,
            cardsByTab = mapOf(
                CollectiblesInventoryTab.Buyback to buybackCards,
                CollectiblesInventoryTab.Collection to collectionCards,
            ),
            refreshTrigger = refreshTrigger,
        )
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = converter.convert(
            selectedTab = selectedTab.value,
            cardsByTab = emptyMap(),
            refreshTrigger = null,
        ),
    )

    init {
        refreshJob = modelScope.launch { inventorySource.refresh() }
    }

    fun onRefresh() {
        refresh(trigger = CollectiblesInventoryRefreshTrigger.PullToRefresh)
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

    @Suppress("EmptyFunctionBlock", "UnusedParameter")
    private fun onCardClick(cardId: String) {
        // TODO([REDACTED_TASK_KEY]): card details are a separate task
    }

    /** `null` until the first result arrives: the tab is still loading. */
    private fun <T> Flow<T>.startWithLoading(): Flow<T?> = map<T, T?> { it }.onStart { emit(null) }
}