package com.tangem.datasource.local.datastore.core

import kotlinx.coroutines.flow.Flow

/**
 * Represents a generic key-value data store.
 *
 * @param Key The type of the keys used to identify values in the store.
 * @param Value The type of the values stored.
 */
internal interface DataStore<Key : Any, Value : Any> {

    /**
     * Checks if the data store is empty.
     *
     * @return `true` if the data store has no entries, otherwise `false`.
     */
    suspend fun isEmpty(): Boolean

    /**
     * Checks if the data store contains an entry with the specified key.
     *
     * @param key The key to check for presence in the store.
     * @return `true` if the key is present, otherwise `false`.
     */
    suspend fun contains(key: Key): Boolean

    /**
     * Retrieves a value updates associated with the given key, as a flow.
     *
     * @param key The key to look up in the store.
     * @return A flow emitting the value associated with the given key.
     */
    fun get(key: Key): Flow<Value>

    /**
     * Retrieves all values updates from the data store, as a flow.
     *
     * @return A flow emitting a list of all values in the store.
     */
    fun getAll(): Flow<List<Value>>

    /**
     * Retrieves a value associated with the given key synchronously.
     *
     * If the key does not exist, this method returns `null`.
     *
     * @param key The key to look up in the store.
     * @return The value associated with the key, or `null` if not present.
     */
    suspend fun getSyncOrNull(key: Key): Value?

    /**
     * Retrieves all values from the data store synchronously.
     *
     * If the store is empty, this method returns `null`.
     *
     * @return A list of all values in the store, or `null` if empty.
     */
    suspend fun getAllSyncOrNull(): List<Value>?

    /**
     * Stores a value in the data store associated with the given key.
     *
     * @param key The key to associate with the value.
     * @param value The value to store.
     */
    suspend fun store(key: Key, value: Value)

    /**
     * Stores multiple values in the data store with their associated keys.
     *
     * @param values A map of keys to values to store.
     */
    suspend fun store(values: Map<Key, Value>)

    /**
     * Removes a value associated with the given key from the data store.
     *
     * @param key The key of the value to remove.
     */
    suspend fun remove(key: Key)

    /**
     * Removes values associated with the given keys from the data store.
     *
     * @param keys Keys of values to remove.
     */
    suspend fun remove(keys: Collection<Key>)

    /**
     * Clears all entries from the data store.
     */
    suspend fun clear()
}