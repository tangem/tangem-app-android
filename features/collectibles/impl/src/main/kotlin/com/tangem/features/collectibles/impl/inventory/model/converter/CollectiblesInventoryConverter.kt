package com.tangem.features.collectibles.impl.inventory.model.converter

import arrow.core.Either
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryMockSource
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryRefreshTrigger
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventorySearch
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryTab
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM

/** `null` while the tab is still loading */
internal typealias InventoryCards = Either<Throwable, List<CollectiblesInventoryMockSource.Card>>?

internal class CollectiblesInventoryConverter(
    private val tabsConverter: CollectiblesInventoryTabsConverter,
    private val contentConverter: CollectiblesInventoryContentConverter,
    private val onSearchQueryChange: (String) -> Unit,
    private val onSearchActiveChange: (Boolean) -> Unit,
    private val onSearchCloseClick: () -> Unit,
) {

    fun convert(
        selectedTab: CollectiblesInventoryTab,
        cardsByTab: Map<CollectiblesInventoryTab, InventoryCards>,
        refreshTrigger: CollectiblesInventoryRefreshTrigger?,
        search: CollectiblesInventorySearch,
        searchCards: List<CollectiblesInventoryMockSource.Card>,
    ): CollectiblesInventoryUM {
        return CollectiblesInventoryUM(
            tabs = tabsConverter.convert(selectedTab = selectedTab, cardsByTab = cardsByTab),
            contentKey = if (search.isSearchMode) "search" else selectedTab.name,
            content = if (search.isSearchMode) {
                contentConverter.convertSearch(cards = searchCards)
            } else {
                contentConverter.convert(
                    tab = selectedTab,
                    cards = cardsByTab[selectedTab],
                    isReloading = refreshTrigger == CollectiblesInventoryRefreshTrigger.Reload,
                )
            },
            isRefreshing = refreshTrigger == CollectiblesInventoryRefreshTrigger.PullToRefresh,
            search = TangemSearch.State(
                placeholderText = resourceReference(R.string.common_search),
                query = search.query,
                onQueryChange = onSearchQueryChange,
                isActive = search.isActive,
                onActiveChange = onSearchActiveChange,
                onCloseClick = onSearchCloseClick,
            ),
            isSearchMode = search.isSearchMode,
        )
    }
}