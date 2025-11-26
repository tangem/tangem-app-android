package com.tangem.datasource.local.news.trending

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.news.ShortArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private typealias TrendingCache = Map<String, List<ShortArticle>>

internal class DefaultTrendingNewsStore(
    private val store: RuntimeSharedStore<TrendingCache>,
) : TrendingNewsStore {

    override fun get(key: String): Flow<List<ShortArticle>> {
        return store.get().map { it[key].orEmpty() }
    }

    override suspend fun getSyncOrNull(key: String): List<ShortArticle>? {
        return store.getSyncOrNull()?.get(key)
    }

    override suspend fun store(key: String, value: List<ShortArticle>) {
        store.update(emptyMap()) { current ->
            current + (key to value)
        }
    }

    override suspend fun clear() {
        store.store(emptyMap())
    }
}