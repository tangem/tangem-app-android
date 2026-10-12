package com.tangem.data.feed.search.store

import com.tangem.data.feed.search.model.QueryDTO
import kotlinx.coroutines.flow.Flow

internal interface FeedSearchHistoryStore {

    fun getQueries(): Flow<List<QueryDTO>>

    suspend fun saveQuery(query: QueryDTO)

    suspend fun removeQuery(text: String)
}