package com.tangem.features.collectibles.impl.inventory.model.converter

import arrow.core.Either
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryMockSource
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryRefreshTrigger
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryTab
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM

/** `null` while the tab is still loading */
internal typealias InventoryCards = Either<Throwable, List<CollectiblesInventoryMockSource.Card>>?

internal class CollectiblesInventoryConverter(
    private val tabsConverter: CollectiblesInventoryTabsConverter,
    private val contentConverter: CollectiblesInventoryContentConverter,
) {

    // TODO([REDACTED_TASK_KEY]): search is a separate task, the field does nothing yet
    private val search = TangemSearch.State(
        placeholderText = resourceReference(R.string.common_search),
        query = "",
        onQueryChange = {},
        isActive = false,
        onActiveChange = {},
    )

    fun convert(
        selectedTab: CollectiblesInventoryTab,
        cardsByTab: Map<CollectiblesInventoryTab, InventoryCards>,
        refreshTrigger: CollectiblesInventoryRefreshTrigger?,
    ) = CollectiblesInventoryUM(
        tabs = tabsConverter.convert(selectedTab = selectedTab, cardsByTab = cardsByTab),
        contentKey = selectedTab.name,
        content = contentConverter.convert(
            tab = selectedTab,
            cards = cardsByTab[selectedTab],
            isReloading = refreshTrigger == CollectiblesInventoryRefreshTrigger.Reload,
        ),
        isRefreshing = refreshTrigger == CollectiblesInventoryRefreshTrigger.PullToRefresh,
        search = search,
    )
}