package com.tangem.data.feed.search.store

import com.tangem.data.feed.search.model.ItemDTO
import com.tangem.data.feed.search.model.QueryDTO
import kotlinx.coroutines.flow.Flow

internal interface FeedSearchHistoryStore {

    fun getItems(): Flow<List<ItemDTO>>

    fun getQueries(): Flow<List<QueryDTO>>

    suspend fun saveItem(item: ItemDTO)

    suspend fun saveQuery(query: QueryDTO)

    suspend fun removeQuery(text: String)

    suspend fun clear()
}