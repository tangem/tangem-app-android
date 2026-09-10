package com.tangem.datasource.local.markets

import com.tangem.store.datasource.markets.models.response.CoinCategoriesResponse.Category

/**
 * Last successfully loaded coin categories, kept across process death as a fallback for a failed refresh.
 *
 * Reads are stamped: a snapshot is returned only when it was taken for the same country **and** the same
 * API environment. The country decides each category's `restricted` flag, and category ids differ between
 * dev and prod, so serving a snapshot taken elsewhere would silently show the wrong restrictions or
 * unresolvable ids.
 */
interface CoinCategoriesSnapshotStore {

    /** Categories stored for [country] and [environment], or `null` when the stamps do not match */
    suspend fun getSyncOrNull(country: String, environment: String): List<Category>?

    suspend fun store(country: String, environment: String, categories: List<Category>)
}