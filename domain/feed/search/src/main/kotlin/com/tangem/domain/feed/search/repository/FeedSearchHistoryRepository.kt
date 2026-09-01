package com.tangem.domain.feed.search.repository

import com.tangem.domain.feed.search.model.RecentFeedSearchItem
import kotlinx.coroutines.flow.Flow

/**
 * Local history of the feed search screen: results the user opened and queries they typed.
 *
 * Both lists are emitted newest-first and hold at most [MAX_HISTORY_SIZE] entries — saving an entry
 * that is already stored moves it to the front instead of duplicating it, and saving beyond the
 * limit evicts the oldest.
 */
interface FeedSearchHistoryRepository {

    fun getRecentItems(): Flow<List<RecentFeedSearchItem>>

    fun getRecentQueries(): Flow<List<String>>

    suspend fun saveRecentItem(item: RecentFeedSearchItem)

    suspend fun saveRecentQuery(query: String)

    suspend fun removeRecentQuery(query: String)

    suspend fun clear()

    companion object {

        const val MAX_HISTORY_SIZE = 10
    }
}