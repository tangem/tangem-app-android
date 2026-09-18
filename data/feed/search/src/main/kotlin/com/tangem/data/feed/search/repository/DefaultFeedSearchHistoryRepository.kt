package com.tangem.data.feed.search.repository

import com.tangem.data.feed.search.model.QueryDTO
import com.tangem.data.feed.search.store.FeedSearchHistoryStore
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

    override fun getRecentQueries(): Flow<List<String>> {
        return store.getQueries()
            .map { queries -> queries.map(QueryDTO::text) }
            .flowOn(dispatchers.io)
    }

    override suspend fun saveRecentQuery(query: String) = withContext(dispatchers.io) {
        store.saveQuery(QueryDTO(text = query, timestamp = System.currentTimeMillis()))
    }

    override suspend fun removeRecentQuery(query: String) = withContext(dispatchers.io) {
        store.removeQuery(query)
    }
}