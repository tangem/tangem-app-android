package com.tangem.datasource.local.datastore.core

import kotlinx.coroutines.flow.Flow

internal interface DataStore<Key : Any, Value : Any> {

    fun get(key: Key): Flow<Value>

    suspend fun getSyncOrNull(key: Key): Value?

    suspend fun store(key: Key, item: Value)

    suspend fun store(items: Map<Key, Value>)

    suspend fun remove(key: Key)

    suspend fun clear()
}