package com.tangem.domain.feed.search.repository

import kotlinx.coroutines.flow.Flow

/**
 * Local history of the feed search screen: the queries the user typed.
 *
 * Emitted newest-first, at most [MAX_HISTORY_SIZE] entries — saving a query that is already stored
 * moves it to the front instead of duplicating it, and saving beyond the limit evicts the oldest.
 */
interface FeedSearchHistoryRepository {

    fun getRecentQueries(): Flow<List<String>>

    suspend fun saveRecentQuery(query: String)

    suspend fun removeRecentQuery(query: String)

    companion object {

        const val MAX_HISTORY_SIZE = 10
    }
}