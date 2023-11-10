package com.tangem.datasource.local.preferences.utils

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.tangem.datasource.local.preferences.AppPreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/** Get flow of nullable data [T] by string [key] */
inline fun <reified T> AppPreferencesStore.getObject(key: Preferences.Key<String>): Flow<T?> {
    val adapter = moshi.adapter(T::class.java)
    return data.map { it[key]?.let(adapter::fromJson) }
}

/** Get flow of data [T] by string [key]. If data is not found, it returns [default] */
inline fun <reified T> AppPreferencesStore.getObject(key: Preferences.Key<String>, default: T): Flow<T> {
    val adapter = moshi.adapter(T::class.java)
    return data.map { it[key]?.let(adapter::fromJson) ?: default }
}

/** Get nullable data [T] by string [key] */
suspend inline fun <reified T> AppPreferencesStore.getObjectSyncOrNull(key: Preferences.Key<String>): T? {
    val adapter = moshi.adapter(T::class.java)
    return data.firstOrNull()
        ?.get(key)
        ?.let(adapter::fromJson)
}

/** Get data [T] by string [key]. If data is not found, it returns [default] */
suspend inline fun <reified T> AppPreferencesStore.getObjectSyncOrDefault(
    key: Preferences.Key<String>,
    default: T,
): T {
    val adapter = moshi.adapter(T::class.java)
    return data.firstOrNull()
        ?.get(key)
        ?.let(adapter::fromJson)
        ?: default
}

/** Store data [value] by string [key] */
suspend inline fun <reified T> AppPreferencesStore.storeObject(key: Preferences.Key<String>, value: T) {
    val adapter = moshi.adapter(T::class.java)
    edit { it[key] = adapter.toJson(value) }
}