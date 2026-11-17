package com.tangem.features.collectibles.impl.inventory.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class CollectiblesInventoryTabsConverter(
    private val onTabClick: (CollectiblesInventoryTab) -> Unit,
) {

    fun convert(
        selectedTab: CollectiblesInventoryTab,
        cardsByTab: Map<CollectiblesInventoryTab, InventoryCards>,
    ): ImmutableList<TangemTabItemUM> = CollectiblesInventoryTab.entries.map { tab ->
        TangemTabItemUM.Content(
            id = tab.name,
            label = when (tab) {
                CollectiblesInventoryTab.Buyback -> resourceReference(R.string.collectibles_inventory_buyback_tab)
                CollectiblesInventoryTab.Collection -> resourceReference(R.string.collectibles_inventory_collection_tab)
            },
            counter = cardsByTab[tab]?.getOrNull()?.let { cards -> stringReference(cards.size.toString()) },
            isSelected = tab == selectedTab,
            onClick = { onTabClick(tab) },
        )
    }.toImmutableList()
}