package com.tangem.features.collectibles.impl.inventory.model

/**
 * @property isActive the field has focus
 */
internal data class CollectiblesInventorySearch(val query: String, val isActive: Boolean) {

    /** Results replace the tabs while the field has focus or keeps a query, so they survive a hidden keyboard. */
    val isSearchMode: Boolean
        get() = isActive || query.isNotEmpty()
}