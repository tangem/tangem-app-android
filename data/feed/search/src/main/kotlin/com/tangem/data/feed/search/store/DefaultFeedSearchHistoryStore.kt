package com.tangem.data.feed.search.store

import androidx.datastore.core.DataStore
import com.tangem.data.feed.search.model.FeedSearchHistoryDTO
import com.tangem.data.feed.search.model.QueryDTO
import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository.Companion.MAX_HISTORY_SIZE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultFeedSearchHistoryStore(
    private val dataStore: DataStore<FeedSearchHistoryDTO>,
) : FeedSearchHistoryStore {

    override fun getQueries(): Flow<List<QueryDTO>> {
        return dataStore.data.map { it.queries.sortedByDescending(QueryDTO::timestamp) }
    }

    override suspend fun saveQuery(query: QueryDTO) {
        dataStore.updateData { current ->
            val withoutSame = current.queries.filterNot { it.text == query.text }

            current.copy(queries = (listOf(query) + withoutSame).take(MAX_HISTORY_SIZE))
        }
    }

    override suspend fun removeQuery(text: String) {
        dataStore.updateData { current ->
            current.copy(queries = current.queries.filterNot { it.text == text })
        }
    }
}