package com.tangem.datasource.local.datastore

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import kotlinx.coroutines.flow.*

internal class RuntimeDataStore<Data : Any> : StringKeyDataStore<Data> {

    private val store: MutableSharedFlow<HashMap<String, Data>?> = MutableSharedFlow(replay = 1)

    init {
        store.tryEmit(value = null)
    }

    override suspend fun isEmpty(): Boolean = store.firstOrNull().isNullOrEmpty()

    override suspend fun contains(key: String): Boolean = getSyncOrNull(key) != null

    override fun get(key: String): Flow<Data> {
        return store.mapNotNull { value ->
            value?.get(key)
        }
    }

    override fun getAll(): Flow<List<Data>> {
        return store.map { value ->
            value?.values?.toList().orEmpty()
        }
    }

    override suspend fun getSyncOrNull(key: String): Data? {
        return store.firstOrNull()?.get(key)
    }

    override suspend fun getAllSyncOrNull(): List<Data>? {
        return store.firstOrNull()?.values?.toList()
    }

    override suspend fun store(key: String, value: Data) {
        updateValue { storedValue ->
            storedValue[key] = value

            storedValue
        }
    }

    override suspend fun store(values: Map<String, Data>) {
        updateValue { storedValue ->
            storedValue.putAll(values)

            storedValue
        }
    }

    override suspend fun remove(key: String) {
        updateValue { storedValue ->
            storedValue.remove(key)

            storedValue
        }
    }

    override suspend fun remove(keys: Collection<String>) {
        updateValue { value ->
            HashMap(value.filterKeys { it !in keys })
        }
    }

    override suspend fun clear() {
        store.emit(value = null)
    }

    private suspend inline fun updateValue(update: (HashMap<String, Data>) -> HashMap<String, Data>) {
        val storedData = store.firstOrNull() ?: hashMapOf()
        val updatedData = update(storedData)

        store.emit(updatedData)
    }
}