package com.tangem.datasource.local.news.viewed

import kotlinx.coroutines.flow.Flow

/**
 * Store for news viewed flags (runtime only).
 */
interface NewsViewedStore {

    /**
     * Observes all viewed flags.
     */
    fun getAll(): Flow<Map<Int, Boolean>>

    /**
     * Gets viewed flags synchronously (returns empty map if no data).
     */
    suspend fun getSync(): Map<Int, Boolean>

    /**
     * Updates viewed flags for provided article ids.
     */
    suspend fun updateViewed(articleIds: Collection<Int>, viewed: Boolean)
}