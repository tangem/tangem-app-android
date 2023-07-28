package com.tangem.datasource.local.datastore.core

import kotlinx.coroutines.flow.Flow

internal abstract class StringKeyDataStoreDecorator<Key : Any, Value : Any>(
    private val dataStore: StringKeyDataStore<Value>,
) : DataStore<Key, Value> {

    abstract fun provideStringKey(key: Key): String

    override fun get(key: Key): Flow<Value> {
        return dataStore.get(provideStringKey(key))
    }

    override suspend fun getSyncOrNull(key: Key): Value? {
        return dataStore.getSyncOrNull(provideStringKey(key))
    }

    override suspend fun store(key: Key, item: Value) {
        dataStore.store(provideStringKey(key), item)
    }

    override suspend fun store(items: Map<Key, Value>) {
        dataStore.store(
            items = items.mapKeys { (key, _) -> provideStringKey(key) },
        )
    }

    override suspend fun remove(key: Key) {
        dataStore.remove(provideStringKey(key))
    }

    override suspend fun clear() {
        dataStore.clear()
    }
}