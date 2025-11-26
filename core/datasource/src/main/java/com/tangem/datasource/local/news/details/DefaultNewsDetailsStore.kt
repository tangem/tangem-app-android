package com.tangem.datasource.local.news.details

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.news.DetailedArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultNewsDetailsStore(
    private val store: RuntimeSharedStore<Map<Int, DetailedArticle>>,
) : NewsDetailsStore {

    override fun getAll(): Flow<List<DetailedArticle>> {
        return store.get().map { it.values.toList() }
    }

    override suspend fun getSyncOrNull(id: Int): DetailedArticle? {
        return store.getSyncOrNull()?.get(id)
    }

    override suspend fun store(id: Int, article: DetailedArticle) {
        store.update(emptyMap()) { current ->
            current + (id to article)
        }
    }

    override suspend fun store(articles: Map<Int, DetailedArticle>) {
        if (articles.isEmpty()) return

        store.update(emptyMap()) { current ->
            current + articles
        }
    }

    override suspend fun clear() {
        store.store(emptyMap())
    }
}