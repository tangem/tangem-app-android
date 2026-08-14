package com.tangem.test.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Creates a real [AppPreferencesStore] backed by [preferencesDataStore] (usually an in-memory
 * [MockStateDataStore]) for use in unit tests.
 *
 * The production implementation is internal, so tests build the public contract directly through this
 * factory instead of depending on the concrete type.
 */
fun createAppPreferencesStore(
    moshi: Moshi,
    dispatchers: CoroutineDispatcherProvider,
    preferencesDataStore: DataStore<Preferences>,
): AppPreferencesStore = object :
    AppPreferencesStore(moshi, dispatchers),
    DataStore<Preferences> by preferencesDataStore {}