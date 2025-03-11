package com.tangem.datasource.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Application preferences store.
 * AppPreferencesStore is wrapper around DataStore<Preferences> that supports json serialization and deserialization.
 *
 * @property moshi                Moshi instance. Property has 'public' modifier because it is used
 *                                  by Public-API inline function. Don't use it directly.
 * @property preferencesDataStore DataStore<Preferences> instance
 *
[REDACTED_AUTHOR]
 */
class AppPreferencesStore(
    val moshi: Moshi,
    private val preferencesDataStore: DataStore<Preferences>,
) : DataStore<Preferences> by preferencesDataStore {

    /**
     * Edit data according with transaction [transform].
     *
     * @param transform transaction. It has receiver [AppPreferencesStore] that allows to use [getObject], [setObject]
     *                    functions when creating transaction.
     */
    suspend fun editData(transform: suspend AppPreferencesStore.(MutablePreferences) -> Unit): Preferences {
        return edit { transform(it) }
    }

    /** Get data [T] by [key]. If data is not found, it returns [default] */
    inline fun <reified T> MutablePreferences.getOrDefault(key: Preferences.Key<T>, default: T): T {
        return this[key] ?: default
    }

    /**
     * Get nullable data [T] by string [key] from [MutablePreferences]
     *
     * Warning: This method cannot be used for T with parameterized types (e.g. List, Set, etc.).
     *
     * @see getObjectList
     *  */
    inline fun <reified T> MutablePreferences.getObject(key: Preferences.Key<String>): T? {
        val adapter = moshi.adapter(T::class.java)
        return this[key]?.let(adapter::fromJson)
    }

    /** Get list of data [T] by string [key] */
    inline fun <reified T> MutablePreferences.getObjectList(key: Preferences.Key<String>): List<T>? {
        val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java))
        return this[key]?.let(adapter::fromJson)
    }

    /** Get list of data [T] by string [key] or default */
    inline fun <reified T> MutablePreferences.getObjectListOrDefault(
        key: Preferences.Key<String>,
        default: List<T>,
    ): List<T> {
        val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java))
        return this[key]?.let(adapter::fromJson) ?: default
    }

    /** Get map with [String] key and value [V] by string [key] from [MutablePreferences] */
    inline fun <reified V> MutablePreferences.getObjectMap(key: Preferences.Key<String>): Map<String, V> {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
        val adapter = moshi.adapter<Map<String, V>>(type)

        return this[key]?.let(adapter::fromJson).orEmpty()
    }

    /** Get set of data [T] by string [key] */
    inline fun <reified T> MutablePreferences.getObjectSet(key: Preferences.Key<String>): Set<T>? {
        val adapter = moshi.adapter<Set<T>>(Types.newParameterizedType(Set::class.java, T::class.java))
        return this[key]?.let(adapter::fromJson)
    }

    /**
     * Set data [T] by string [key] to [MutablePreferences]
     *
     * Warning: This method cannot be used for T with parameterized types (e.g. List, Set, etc.).
     *
     * @see setObjectList
     * */
    inline fun <reified T> MutablePreferences.setObject(key: Preferences.Key<String>, value: T) {
        val adapter = moshi.adapter(T::class.java) // TODO: Support parameterized types
        this[key] = adapter.toJson(value)
    }

    /** Set list of data [T] by string [key] to [MutablePreferences] */
    inline fun <reified T> MutablePreferences.setObjectList(key: Preferences.Key<String>, value: List<T>) {
        val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java))
        this[key] = adapter.toJson(value)
    }

    /** Set map with [String] key and value [V] by string [key] to [MutablePreferences] */
    inline fun <reified V> MutablePreferences.setObjectMap(key: Preferences.Key<String>, value: Map<String, V>) {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
        val adapter = moshi.adapter<Map<String, V>>(type)

        this[key] = adapter.toJson(value)
    }

    /** Sets set of data [T] by string [key] to [MutablePreferences] */
    inline fun <reified T> MutablePreferences.setObjectSet(key: Preferences.Key<String>, value: Set<T>) {
        val adapter = moshi.adapter<Set<T>>(Types.newParameterizedType(Set::class.java, T::class.java))
        this[key] = adapter.toJson(value)
    }
}