package com.tangem.data.news.store.liked

import kotlinx.coroutines.flow.Flow

/**
 * Store for news liked flags (runtime only).
 */
internal interface NewsLikedStore {

    /**
     * Observes all liked flags.
     */
    fun getAll(): Flow<Map<Int, Boolean>>

    /**
     * Gets liked flags synchronously (returns empty map if no data).
     */
    suspend fun getSync(): Map<Int, Boolean>

    /**
     * Updates liked flags for provided article ids.
     */
    suspend fun updateLiked(articleIds: Collection<Int>, liked: Boolean)
}