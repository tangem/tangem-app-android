package com.tangem.datasource.local.datastore.core

internal abstract class SingleStringKeyDataStoreDecorator<Value : Any>(
    wrappedDataStore: StringKeyDataStore<Value>,
    private val key: String,
) : StringKeyDataStoreDecorator<String, Value>(wrappedDataStore) {

    override fun provideStringKey(key: String) = key

    open fun get() = get(key)

    open suspend fun getSyncOrNull() = getSyncOrNull(key)

    open suspend fun store(item: Value) = store(key, item)

    open suspend fun isEmpty() = getSyncOrNull() == null
}
