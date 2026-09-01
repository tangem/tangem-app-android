package com.tangem.data.feed.search.repository

import com.tangem.data.feed.search.converter.toDomain
import com.tangem.data.feed.search.converter.toDTO
import com.tangem.data.feed.search.model.ItemDTO
import com.tangem.data.feed.search.model.QueryDTO
import com.tangem.data.feed.search.store.FeedSearchHistoryStore
import com.tangem.domain.feed.search.model.RecentFeedSearchItem
import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DefaultFeedSearchHistoryRepository(
    private val store: FeedSearchHistoryStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : FeedSearchHistoryRepository {

    override fun getRecentItems(): Flow<List<RecentFeedSearchItem>> {
        return store.getItems()
            .map { items -> items.mapNotNull(ItemDTO::toDomain) }
            .flowOn(dispatchers.io)
    }

    override fun getRecentQueries(): Flow<List<String>> {
        return store.getQueries()
            .map { queries -> queries.map(QueryDTO::text) }
            .flowOn(dispatchers.io)
    }

    override suspend fun saveRecentItem(item: RecentFeedSearchItem) = withContext(dispatchers.io) {
        store.saveItem(item.toDTO(timestamp = System.currentTimeMillis()))
    }

    override suspend fun saveRecentQuery(query: String) = withContext(dispatchers.io) {
        store.saveQuery(QueryDTO(text = query, timestamp = System.currentTimeMillis()))
    }

    override suspend fun removeRecentQuery(query: String) = withContext(dispatchers.io) {
        store.removeQuery(query)
    }

    override suspend fun clear() = withContext(dispatchers.io) {
        store.clear()
    }
}