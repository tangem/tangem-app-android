package com.tangem.datasource.local.datastore.core

import kotlinx.coroutines.flow.Flow

internal abstract class KeylessDataStoreDecorator<Value : Any>(
    wrappedDataStore: StringKeyDataStore<Value>,
) : StringKeyDataStoreDecorator<Unit, Value>(wrappedDataStore) {

    override fun provideStringKey(key: Unit): String {
        return STRING_KEY
    }

    fun get(): Flow<Value> {
        return get(Unit)
    }

    suspend fun getSyncOrNull(): Value? {
        return getSyncOrNull(Unit)
    }

    suspend fun store(item: Value) {
        store(Unit, item)
    }

    private companion object {
        const val STRING_KEY = "key"
    }
}
