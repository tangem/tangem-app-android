package com.tangem.datasource.local.news.trending

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.news.TrendingNews
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private typealias TrendingCache = Map<String, TrendingNews>

internal class DefaultTrendingNewsStore(
    private val store: RuntimeSharedStore<TrendingCache>,
) : TrendingNewsStore {

    override fun get(key: String): Flow<TrendingNews> {
        return store.get().map { it[key] ?: TrendingNews.Data(emptyList()) }
    }

    override suspend fun getSyncOrNull(key: String): TrendingNews? {
        return store.getSyncOrNull()?.get(key)
    }

    override suspend fun store(key: String, value: TrendingNews) {
        store.update(emptyMap()) { current ->
            current + (key to value)
        }
    }

    override suspend fun clear() {
        store.store(emptyMap())
    }
}