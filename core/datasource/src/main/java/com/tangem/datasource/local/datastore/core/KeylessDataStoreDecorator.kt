package com.tangem.datasource.local.datastore.core

import kotlinx.coroutines.flow.Flow

internal abstract class KeylessDataStoreDecorator<Value : Any>(
    wrappedDataStore: StringKeyDataStore<Value>,
) : StringKeyDataStoreDecorator<Unit, Value>(wrappedDataStore) {

    override fun provideStringKey(key: Unit): String {
        return STRING_KEY
    }

    open fun get(): Flow<Value> {
        return get(Unit)
    }

    open suspend fun getSyncOrNull(): Value? {
        return getSyncOrNull(Unit)
    }

    open suspend fun store(item: Value) {
        store(Unit, item)
    }

    private companion object {
        const val STRING_KEY = "key"
    }
}