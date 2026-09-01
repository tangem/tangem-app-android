package com.tangem.data.feed.search.store

import androidx.datastore.core.DataStore
import com.tangem.data.feed.search.model.FeedSearchHistoryDTO
import com.tangem.data.feed.search.model.ItemDTO
import com.tangem.data.feed.search.model.QueryDTO
import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository.Companion.MAX_HISTORY_SIZE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultFeedSearchHistoryStore(
    private val dataStore: DataStore<FeedSearchHistoryDTO>,
) : FeedSearchHistoryStore {

    override fun getItems(): Flow<List<ItemDTO>> {
        return dataStore.data.map { it.items.sortedByDescending(ItemDTO::timestamp) }
    }

    override fun getQueries(): Flow<List<QueryDTO>> {
        return dataStore.data.map { it.queries.sortedByDescending(QueryDTO::timestamp) }
    }

    override suspend fun saveItem(item: ItemDTO) {
        dataStore.updateData { current ->
            // kinds are independent namespaces: a token and an article may share an id
            val withoutSame = current.items.filterNot { it.kind == item.kind && it.id == item.id }

            current.copy(items = withoutSame.prependAndCap(item))
        }
    }

    override suspend fun saveQuery(query: QueryDTO) {
        dataStore.updateData { current ->
            val withoutSame = current.queries.filterNot { it.text == query.text }

            current.copy(queries = withoutSame.prependAndCap(query))
        }
    }

    override suspend fun removeQuery(text: String) {
        dataStore.updateData { current ->
            current.copy(queries = current.queries.filterNot { it.text == text })
        }
    }

    override suspend fun clear() {
        dataStore.updateData { FeedSearchHistoryDTO() }
    }

    private fun <T> List<T>.prependAndCap(entry: T): List<T> = (listOf(entry) + this).take(MAX_HISTORY_SIZE)
}