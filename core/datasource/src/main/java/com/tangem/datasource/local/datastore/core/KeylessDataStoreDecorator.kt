package com.tangem.datasource.local.datastore.core

internal abstract class KeylessDataStoreDecorator<Value : Any>(
    wrappedDataStore: StringKeyDataStore<Value>,
    private val key: String = DEFAULT_STRING_KEY,
) : StringKeyDataStoreDecorator<String, Value>(wrappedDataStore) {

    override fun provideStringKey(key: String) = key

    open fun get() = get(key)

    open suspend fun getSyncOrNull() = getSyncOrNull(key)

    open suspend fun store(item: Value) = store(key, item)

    open suspend fun isEmpty() = getSyncOrNull() == null

    private companion object {
        const val DEFAULT_STRING_KEY = "key"
    }
}