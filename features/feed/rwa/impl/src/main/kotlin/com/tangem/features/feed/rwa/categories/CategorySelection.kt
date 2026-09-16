package com.tangem.features.feed.rwa.categories

import kotlinx.serialization.Serializable

/**
 * A category filter applied by the user.
 *
 * @property category picked category; `null` clears the filter ("All")
 * @property sector picked sector within [category]; `null` means the whole category
 */
@Serializable
internal data class CategorySelection(
    val category: Entry? = null,
    val sector: Entry? = null,
) {

    /**
     * @property id   identity of the pick: what the selector matches on and what the list request sends
     * @property code stable key the localized name is resolved from, via [RwaCategoryNames]
     */
    @Serializable
    data class Entry(val id: String, val displayName: String, val code: String)
}