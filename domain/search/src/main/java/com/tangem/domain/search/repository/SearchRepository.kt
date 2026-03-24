package com.tangem.domain.search.repository

import com.tangem.domain.search.model.RecentSearchToken
import com.tangem.domain.search.model.SearchTextHint
import kotlinx.coroutines.flow.Flow

/**
 * Repository responsible for managing local search history storage.
 * Handles persistence of user's past search queries and recently viewed market tokens.
 * Each history type is limited to 3 entries, sorted by timestamp in descending order.
 */
interface SearchRepository {

    /** Observes the list of saved text hints, sorted by timestamp descending. */
    fun getTextHints(): Flow<List<SearchTextHint>>

    /** Observes the list of recently viewed market tokens, sorted by timestamp descending. */
    fun getRecentTokens(): Flow<List<RecentSearchToken>>

    /**
     * Saves a text hint to the search history.
     * If the hint already exists, its timestamp is updated. Oldest entries are evicted when the limit is exceeded.
     *
     * @param text the search query text to save
     */
    suspend fun saveTextHint(text: String)

    /**
     * Saves a recently viewed market token to the search history.
     * If a token with the same ID already exists, it is moved to the top. Oldest entries are evicted when the limit
     * is exceeded.
     *
     * @param token the market token entry to save
     */
    suspend fun saveRecentToken(token: RecentSearchToken)

    /** Clears all search history, including both text hints and recent tokens. */
    suspend fun clearHistory()
}