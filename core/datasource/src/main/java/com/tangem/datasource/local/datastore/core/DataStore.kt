package com.tangem.datasource.local.datastore.core

import kotlinx.coroutines.flow.Flow

internal interface DataStore<Key : Any, Value : Any> {

    fun get(key: Key): Flow<Value>

    fun getAll(): Flow<List<Value>>

    suspend fun getSyncOrNull(key: Key): Value?

    suspend fun getAllSyncOrNull(): List<Value>?

    suspend fun store(key: Key, value: Value)

    suspend fun store(values: Map<Key, Value>)

    suspend fun remove(key: Key)

    suspend fun clear()
}