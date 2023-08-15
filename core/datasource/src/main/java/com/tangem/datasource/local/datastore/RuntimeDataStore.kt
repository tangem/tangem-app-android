package com.tangem.datasource.local.datastore

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import kotlinx.coroutines.flow.*

internal class RuntimeDataStore<Data : Any> : StringKeyDataStore<Data> {

    private val store = MutableStateFlow<HashMap<String, Data>>(hashMapOf())

    override fun get(key: String): Flow<Data> {
        return store
            .map { value -> value[key] }
            .filterNotNull()
    }

    override fun getAll(): Flow<List<Data>> {
        return store.map { value -> value.values.toList() }
    }

    override suspend fun getSyncOrNull(key: String): Data? {
        return store.value[key]
    }

    override suspend fun getAllSyncOrNull(): List<Data> {
        return store.value.values.toList()
    }

    override suspend fun store(key: String, item: Data) {
        store.update { value ->
            value[key] = item

            value
        }
    }

    override suspend fun store(items: Map<String, Data>) {
        store.update { value ->
            items.forEach { (key, item) ->
                value[key] = item
            }

            value
        }
    }

    override suspend fun remove(key: String) {
        store.update { value ->
            value.remove(key)

            value
        }
    }

    override suspend fun clear() {
        store.update { hashMapOf() }
    }
}