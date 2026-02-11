package com.tangem.datasource.local.news.viewed

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

private typealias NewsViewedCache = Map<Int, Boolean>

internal class DefaultNewsViewedStore(
    private val store: RuntimeSharedStore<NewsViewedCache>,
) : NewsViewedStore {

    override fun getAll(): Flow<Map<Int, Boolean>> {
        return store.get().onStart { emit(emptyMap()) }
    }

    override suspend fun getSync(): Map<Int, Boolean> {
        return store.getSyncOrNull().orEmpty()
    }

    override suspend fun updateViewed(articleIds: Collection<Int>, viewed: Boolean) {
        if (articleIds.isEmpty()) return

        store.update(emptyMap()) { current ->
            val updated = current.toMutableMap()
            articleIds.forEach { id ->
                updated[id] = viewed
            }
            updated
        }
    }
}