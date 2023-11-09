package com.tangem.datasource.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.squareup.moshi.Moshi

/**
 * Application preferences store.
 * AppPreferencesStore is wrapper around DataStore<Preferences> that supports json serialization and deserialization.
 *
 * @property moshi                Moshi instance. Property has 'public' modifier because it is used
 *                                  by Public-API inline function. Don't use it directly.
 * @property preferencesDataStore DataStore<Preferences> instance
 *
* [REDACTED_AUTHOR]
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

    /** Get nullable data [T] by string [key] from [MutablePreferences] */
    inline fun <reified T> MutablePreferences.getObject(key: Preferences.Key<String>): T? {
        val adapter = moshi.adapter(T::class.java)
        return this[key]?.let(adapter::fromJson)
    }

    /** Set data [T] by string [key] to [MutablePreferences] */
    inline fun <reified T> MutablePreferences.setObject(key: Preferences.Key<String>, value: T) {
        val adapter = moshi.adapter(T::class.java)
        this[key] = adapter.toJson(value)
    }
}
