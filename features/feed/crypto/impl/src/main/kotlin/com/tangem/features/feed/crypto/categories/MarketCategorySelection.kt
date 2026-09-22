package com.tangem.features.feed.crypto.categories

import kotlinx.serialization.Serializable

/**
 * Category filter applied to the Market Pulse list.
 *
 * The empty instance is the one canonical "no filter": the tab chip reads "All", the selector checks
 * its "All" row, and no `geckoCategoryId` travels with the list request. Gecko categories have no
 * sectors, so a selection is a single optional entry.
 */
@Serializable
internal data class MarketCategorySelection(val category: Entry? = null) {

    @Serializable
    data class Entry(val id: String, val name: String)
}