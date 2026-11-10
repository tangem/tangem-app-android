package com.tangem.features.feed.rwa.categories.model

import com.tangem.domain.markets.CoinCategory

/**
 * A lone sector filters no differently than its category, so a sector choice exists only from two
 * sectors up. Below that the category row is the whole filter: the selector shows no Sectors rail and a
 * tap on the category applies it at once.
 */
internal val CoinCategory.hasSectorChoice: Boolean
    get() = sectors.size > 1