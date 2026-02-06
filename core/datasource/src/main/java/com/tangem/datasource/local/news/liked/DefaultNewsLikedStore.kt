package com.tangem.datasource.local.news.liked

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

private typealias NewsLikedCache = Map<Int, Boolean>

internal class DefaultNewsLikedStore(
    private val store: RuntimeSharedStore<NewsLikedCache>,
) : NewsLikedStore {

    override fun getAll(): Flow<Map<Int, Boolean>> {
        return store.get().onStart { emit(emptyMap()) }
    }

    override suspend fun getSync(): Map<Int, Boolean> {
        return store.getSyncOrNull().orEmpty()
    }

    override suspend fun updateLiked(articleIds: Collection<Int>, liked: Boolean) {
        if (articleIds.isEmpty()) return

        store.update(emptyMap()) { current ->
            val updated = current.toMutableMap()
            articleIds.forEach { id ->
                updated[id] = liked
            }
            updated
        }
    }
}