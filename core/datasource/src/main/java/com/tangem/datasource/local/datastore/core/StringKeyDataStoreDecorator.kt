package com.tangem.datasource.local.datastore.core

import kotlinx.coroutines.flow.Flow

internal abstract class StringKeyDataStoreDecorator<Key : Any, Value : Any>(
    private val wrappedDataStore: StringKeyDataStore<Value>,
) : DataStore<Key, Value> {

    abstract fun provideStringKey(key: Key): String

    override suspend fun isEmpty(): Boolean = wrappedDataStore.isEmpty()

    override suspend fun contains(key: Key): Boolean = wrappedDataStore.contains(provideStringKey(key))

    override fun get(key: Key): Flow<Value> {
        return wrappedDataStore.get(provideStringKey(key))
    }

    override fun getAll(): Flow<List<Value>> {
        return wrappedDataStore.getAll()
    }

    override suspend fun getAllSyncOrNull(): List<Value>? {
        return wrappedDataStore.getAllSyncOrNull()
    }

    override suspend fun getSyncOrNull(key: Key): Value? {
        return wrappedDataStore.getSyncOrNull(provideStringKey(key))
    }

    override suspend fun store(key: Key, value: Value) {
        wrappedDataStore.store(provideStringKey(key), value)
    }

    override suspend fun store(values: Map<Key, Value>) {
        wrappedDataStore.store(
            values = values.mapKeys { (key, _) -> provideStringKey(key) },
        )
    }

    override suspend fun remove(key: Key) {
        wrappedDataStore.remove(provideStringKey(key))
    }

    override suspend fun remove(keys: Collection<Key>) {
        wrappedDataStore.remove(keys.map(::provideStringKey))
    }

    override suspend fun clear() {
        wrappedDataStore.clear()
    }
}