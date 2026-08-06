package com.tangem.core.local.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * Runtime key-value store backed by a single [RuntimeSharedStore] holding a [Map].
 *
 * All mutations are serialized through [RuntimeSharedStore.update], so concurrent writes cannot
 * clobber each other. The key type [K] is generic, so callers keep their own key type without
 * mapping it to a [String].
 *
 * @param K type of the keys
 * @param V type of the values
 */
interface RuntimeSharedMapStore<K : Any, V : Any> {

    /** Get flow of values associated with [key], skipping updates where the key is absent */
    fun get(key: K): Flow<V>

    /** Get flow of all values */
    fun getAll(): Flow<List<V>>

    /** Get value associated with [key] synchronously or null */
    suspend fun getSyncOrNull(key: K): V?

    /** Get all values synchronously, or null if nothing has ever been stored (an empty list after [clear]) */
    suspend fun getAllSyncOrNull(): List<V>?

    /** Check whether a value is associated with [key] */
    suspend fun contains(key: K): Boolean

    /** Store [value] under [key] */
    suspend fun store(key: K, value: V)

    /** Store all [values] */
    suspend fun store(values: Map<K, V>)

    /**
     * Atomically update the value associated with [key].
     *
     * The read-modify-write runs under the store's mutex, so concurrent updates of the same [key]
     * cannot lose each other's changes. [transform] receives the current value, or [default] when the
     * key is absent.
     */
    suspend fun update(key: K, default: V, transform: (V) -> V)

    /**
     * Update the value under [key] only if it is already present.
     *
     * The presence check and the mutation both run under the store's mutex, so a concurrent insert of

     * the mutex is a short-circuit when the store has never been written to — that keeps
     * [getAllSyncOrNull] returning null if nothing has ever been stored.
     */
    suspend fun updateIfPresent(key: K, transform: (V) -> V)

    /** Remove value associated with [key] */
    suspend fun remove(key: K)

    /** Remove values associated with [keys] */
    suspend fun remove(keys: Collection<K>)

    /** Clear all stored values */
    suspend fun clear()

    companion object {

        /** Create [RuntimeSharedMapStore] */
        operator fun <K : Any, V : Any> invoke(): RuntimeSharedMapStore<K, V> = object : RuntimeSharedMapStore<K, V> {

            private val store = RuntimeSharedStore<Map<K, V>>()

            override fun get(key: K): Flow<V> = store.get().mapNotNull { it[key] }

            override fun getAll(): Flow<List<V>> = store.get().map { it.values.toList() }

            override suspend fun getSyncOrNull(key: K): V? = store.getSyncOrNull()?.get(key)

            override suspend fun getAllSyncOrNull(): List<V>? = store.getSyncOrNull()?.values?.toList()

            override suspend fun contains(key: K): Boolean = store.getSyncOrNull()?.containsKey(key) == true

            override suspend fun store(key: K, value: V) {
                store.update(default = emptyMap()) { it + (key to value) }
            }

            override suspend fun store(values: Map<K, V>) {
                store.update(default = emptyMap()) { it + values }
            }

            override suspend fun update(key: K, default: V, transform: (V) -> V) {
                store.update(default = emptyMap()) { map ->
                    map + (key to transform(map[key] ?: default))
                }
            }

            override suspend fun updateIfPresent(key: K, transform: (V) -> V) {
                if (store.getSyncOrNull() == null) return

                store.update(default = emptyMap()) { map ->
                    val current = map[key] ?: return@update map
                    map + (key to transform(current))
                }
            }

            override suspend fun remove(key: K) {
                store.update(default = emptyMap()) { it - key }
            }

            override suspend fun remove(keys: Collection<K>) {
                store.update(default = emptyMap()) { it - keys }
            }

            override suspend fun clear() {
                store.update(default = emptyMap()) { emptyMap() }
            }
        }
    }
}