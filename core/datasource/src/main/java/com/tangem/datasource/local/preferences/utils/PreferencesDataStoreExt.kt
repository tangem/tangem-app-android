package com.tangem.datasource.local.preferences.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/** Get flow of nullable data [T] by [key] */
fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): Flow<T?> {
    return data.map { it[key] }
}

/** Get flow of data [T] by [key]. If data is not found, it returns [default] */
fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>, default: T): Flow<T> {
    return data.map { it[key] ?: default }
}

/** Get nullable data [T] by [key] */
suspend fun <T> DataStore<Preferences>.getSyncOrNull(key: Preferences.Key<T>): T? {
    return data.firstOrNull()?.get(key)
}

/** Get data [T] by [key]. If data is not found, it returns [default] */
suspend fun <T> DataStore<Preferences>.getSyncOrDefault(key: Preferences.Key<T>, default: T): T {
    return data.firstOrNull()?.get(key) ?: default
}

/** Store data [value] by [key] */
suspend fun <T> DataStore<Preferences>.store(key: Preferences.Key<T>, value: T) {
    edit { it[key] = value }
}