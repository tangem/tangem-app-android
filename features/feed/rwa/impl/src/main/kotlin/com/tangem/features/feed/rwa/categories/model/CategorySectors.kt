package com.tangem.features.feed.rwa.categories.model

import com.tangem.domain.markets.CoinCategory
import com.tangem.features.feed.rwa.categories.CategorySelection

/**
 * A lone sector filters no differently than its category, so a sector choice exists only from two
 * sectors up. Below that the category row is the whole filter: the selector shows no sector chips under it.
 */
internal val CoinCategory.hasSectorChoice: Boolean
    get() = sectors.size > 1

internal fun CoinCategory.toSelectionEntry(): CategorySelection.Entry = CategorySelection.Entry(
    id = id,
    displayName = displayName,
    code = code,
)