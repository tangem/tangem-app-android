package com.tangem.datasource.local.preferences.utils

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Types
import com.tangem.datasource.local.preferences.AppPreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/** Get flow of nullable data [T] by string [key] */
inline fun <reified T> AppPreferencesStore.getObject(key: Preferences.Key<String>): Flow<T?> {
    val adapter = moshi.adapter(T::class.java)
    return data.map { preferences ->
        preferences[key]?.let {
            try {
                adapter.fromJson(it)
            } catch (e: JsonDataException) {
                null
            }
        }
    }.distinctUntilChanged()
}

/**
 * Get flow of data [T] by string [key]. If data is not found, it returns [default]
 *
 * Warning: This method cannot be used for parameterized types (e.g. List, Set, etc.).
 *
 * @see getObjectList
 * */
inline fun <reified T> AppPreferencesStore.getObject(key: Preferences.Key<String>, default: T): Flow<T> {
    val adapter = moshi.adapter(T::class.java) // TODO: Support parameterized types
    return data.map {
        try {
            it[key]?.let(adapter::fromJson) ?: default
        } catch (e: JsonDataException) {
            default
        }
    }.distinctUntilChanged()
}

/**
 * Get nullable data [T] by string [key]
 *
 * Warning: This method cannot be used for T with parameterized types (e.g. List, Set, etc.).
 *
 * @see getObjectListSync
 * */
suspend inline fun <reified T> AppPreferencesStore.getObjectSyncOrNull(key: Preferences.Key<String>): T? {
    val adapter = moshi.adapter(T::class.java) // TODO: Support parameterized types
    return data.firstOrNull()
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
): T {
    val adapter = moshi.adapter(T::class.java)
    return data.firstOrNull()
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
suspend inline fun <reified T> AppPreferencesStore.storeObject(key: Preferences.Key<String>, value: T) {
    val adapter = moshi.adapter(T::class.java) // TODO: Support parameterized types
    edit { it[key] = adapter.toJson(value) }
}

/** Store list of data [value] by string [key] */
suspend inline fun <reified T> AppPreferencesStore.storeObjectList(key: Preferences.Key<String>, value: List<T>) {
    val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java))
    edit { it[key] = adapter.toJson(value) }
}

/** Get flow of list of data [T] by string [key]. If data is not found, it returns `null` */
inline fun <reified T> AppPreferencesStore.getObjectList(key: Preferences.Key<String>): Flow<List<T>?> {
    val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java))
    return data.map { it[key]?.let(adapter::fromJson) }.distinctUntilChanged()
}

/** Get list of data [T] by string [key], or empty if data is not found */
suspend inline fun <reified T> AppPreferencesStore.getObjectListSync(key: Preferences.Key<String>): List<T> {
    val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java))
    return data.firstOrNull()
        ?.get(key)
        ?.let(adapter::fromJson)
        .orEmpty()
}

/** Store map with [String] key and value [V] by string [key] */
suspend inline fun <reified V> AppPreferencesStore.storeObjectMap(
    key: Preferences.Key<String>,
    value: Map<String, V>,
) {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
    val adapter = moshi.adapter<Map<String, V>>(type)

    edit { it[key] = adapter.toJson(value) }
}

/** Get map with [String] key and value [V] by string [key], or empty if data is not found */
suspend inline fun <reified V> AppPreferencesStore.getObjectMapSync(key: Preferences.Key<String>): Map<String, V> {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
    val adapter = moshi.adapter<Map<String, V>>(type)

    return data.firstOrNull()
        ?.get(key)
        ?.let(adapter::fromJson)
        .orEmpty()
}

/** Get flow of map with [String] key and value [V] by string [key], or empty if data is not found */
inline fun <reified V> AppPreferencesStore.getObjectMap(key: Preferences.Key<String>): Flow<Map<String, V>> {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
    val adapter = moshi.adapter<Map<String, V>>(type)

    return data.map { it[key]?.let(adapter::fromJson) ?: emptyMap() }
}

/** Get set of data [T] by string [key], or empty if data is not found */
suspend inline fun <reified T> AppPreferencesStore.getObjectSetSync(key: Preferences.Key<String>): Set<T> {
    val adapter = moshi.adapter<Set<T>>(Types.newParameterizedType(Set::class.java, T::class.java))
    return data.firstOrNull()
        ?.get(key)
        ?.let(adapter::fromJson)
        .orEmpty()
}