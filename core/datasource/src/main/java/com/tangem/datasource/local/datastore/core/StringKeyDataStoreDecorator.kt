package com.tangem.datasource.local.datastore.core

import kotlinx.coroutines.flow.Flow

internal abstract class StringKeyDataStoreDecorator<Key : Any, Value : Any>(
    private val wrappedDataStore: StringKeyDataStore<Value>,
) : DataStore<Key, Value> {

    abstract fun provideStringKey(key: Key): String

    override fun get(key: Key): Flow<Value> {
        return wrappedDataStore.get(provideStringKey(key))
    }

    override fun getAll(): Flow<List<Value>> {
        return wrappedDataStore.getAll()
    }

    override suspend fun getAllSyncOrNull(): List<Value> {
        return wrappedDataStore.getAllSyncOrNull()
    }

    override suspend fun getSyncOrNull(key: Key): Value? {
        return wrappedDataStore.getSyncOrNull(provideStringKey(key))
    }

    override suspend fun store(key: Key, item: Value) {
        wrappedDataStore.store(provideStringKey(key), item)
    }

    override suspend fun store(items: Map<Key, Value>) {
        wrappedDataStore.store(
            items = items.mapKeys { (key, _) -> provideStringKey(key) },
        )
    }

    override suspend fun remove(key: Key) {
        wrappedDataStore.remove(provideStringKey(key))
    }

    override suspend fun clear() {
        wrappedDataStore.clear()
    }
}