package com.tangem.datasource.local.preferences.utils

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Types
import com.tangem.datasource.local.preferences.AppPreferencesStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/** Get flow of nullable data [T] by string [key] */
inline fun <reified T> AppPreferencesStore.getObject(key: Preferences.Key<String>): Flow<T?> {
    return flow {
        val adapter = moshi.adapter(T::class.java)
        emitAll(
            data.map { preferences ->
                preferences[key]?.let {
                    try {
                        adapter.fromJson(it)
                    } catch (e: JsonDataException) {
                        null
                    }
                }
            }.distinctUntilChanged(),
        )
    }
}

/**
 * Get flow of data [T] by string [key]. If data is not found, it returns [default]
 *
 * Warning: This method cannot be used for parameterized types (e.g. List, Set, etc.).
 *
 * @see getObjectList
 * */
inline fun <reified T> AppPreferencesStore.getObject(key: Preferences.Key<String>, default: T): Flow<T> {
    return flow {
        val adapter = moshi.adapter(T::class.java)
        emitAll(
            data.map {
                try {
                    it[key]?.let(adapter::fromJson) ?: default
                } catch (e: JsonDataException) {
                    default
                }
            }.distinctUntilChanged(),
        )
    }
}
/**
 * Get nullable data [T] by string [key]
 *
 * Warning: This method cannot be used for T with parameterized types (e.g. List, Set, etc.).
 *
 * @see getObjectListSync
 * */
suspend inline fun <reified T> AppPreferencesStore.getObjectSyncOrNull(key: Preferences.Key<String>): T? =
    withContext(dispatchers.io) {
        val adapter = moshi.adapter(T::class.java) // TODO: Support parameterized types
        data.firstOrNull()
            ?.get(key)
            ?.let {
                try {
                    adapter.fromJson(it)
                } catch (e: JsonDataException) {
                    null
                }
            }
    }

/** Get data [T] by string [key]. If data is not found, it returns [default] */
suspend inline fun <reified T> AppPreferencesStore.getObjectSyncOrDefault(
    key: Preferences.Key<String>,
    default: T,
): T = withContext(dispatchers.io) {
    val adapter = moshi.adapter(T::class.java)
    data.firstOrNull()
        ?.get(key)
        ?.let {
            try {
                adapter.fromJson(it)
            } catch (e: JsonDataException) {
                default
            }
        }
        ?: default
}

/**
 * Store data [value] by string [key]
 *
 * Warning: This method cannot be used for T with parameterized types (e.g. List, Set, etc.).
 *
 * @see storeObjectList
 * */
@Suppress("OptionalUnit")
suspend inline fun <reified T> AppPreferencesStore.storeObject(key: Preferences.Key<String>, value: T): Unit =
    withContext(dispatchers.io) {
        val adapter = moshi.adapter(T::class.java) // TODO: Support parameterized types
        edit { it[key] = adapter.toJson(value) }
    }

/** Store list of data [value] by string [key] */
suspend inline fun <reified T> AppPreferencesStore.storeObjectList(key: Preferences.Key<String>, value: List<T>) =
    withContext(dispatchers.io) {
        val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java))
        edit { it[key] = adapter.toJson(value) }
    }

/** Get flow of list of data [T] by string [key]. If data is not found, it returns `null` */
inline fun <reified T> AppPreferencesStore.getObjectList(key: Preferences.Key<String>): Flow<List<T>?> {
    return flow {
        val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java))
        emitAll(
            data.map {
                it[key]?.let(adapter::fromJson)
            }.distinctUntilChanged(),
        )
    }
}

/** Get list of data [T] by string [key], or empty if data is not found */
suspend inline fun <reified T> AppPreferencesStore.getObjectListSync(key: Preferences.Key<String>): List<T> =
    withContext(dispatchers.io) {
        val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java))
        data.firstOrNull()
            ?.get(key)
            ?.let(adapter::fromJson)
            .orEmpty()
    }

/** Store map with [String] key and value [V] by string [key] */
suspend inline fun <reified V> AppPreferencesStore.storeObjectMap(
    key: Preferences.Key<String>,
    value: Map<String, V>,
) = withContext(dispatchers.io) {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
    val adapter = moshi.adapter<Map<String, V>>(type)

    edit { it[key] = adapter.toJson(value) }
}

/** Get map with [String] key and value [V] by string [key], or empty if data is not found */
suspend inline fun <reified V> AppPreferencesStore.getObjectMapSync(key: Preferences.Key<String>): Map<String, V> =
    withContext(dispatchers.io) {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
        val adapter = moshi.adapter<Map<String, V>>(type)

        data.firstOrNull()
            ?.get(key)
            ?.let(adapter::fromJson)
            .orEmpty()
    }

/** Get flow of map with [String] key and value [V] by string [key], or empty if data is not found */
inline fun <reified V> AppPreferencesStore.getObjectMap(key: Preferences.Key<String>): Flow<Map<String, V>> {
    return flow {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
        val adapter = moshi.adapter<Map<String, V>>(type)

        emitAll(
            data.map { it[key]?.let(adapter::fromJson) ?: emptyMap() },
        )
    }
}

/** Get set of data [T] by string [key], or empty if data is not found */
suspend inline fun <reified T> AppPreferencesStore.getObjectSetSync(key: Preferences.Key<String>): Set<T> =
    withContext(dispatchers.io) {
        val adapter = moshi.adapter<Set<T>>(Types.newParameterizedType(Set::class.java, T::class.java))
        data.firstOrNull()
            ?.get(key)
            ?.let(adapter::fromJson)
            .orEmpty()
    }

/** Get flow of set of [T] by string [key], or empty if data is not found */
inline fun <reified T> AppPreferencesStore.getObjectSet(key: Preferences.Key<String>): Flow<Set<T>> {
    return flow {
        val adapter = moshi.adapter<Set<T>>(Types.newParameterizedType(Set::class.java, T::class.java))
        emitAll(
            data.map {
                it[key]?.let(adapter::fromJson) ?: emptySet()
            },
        )
    }
}