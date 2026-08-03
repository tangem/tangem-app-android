package com.tangem.datasource.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.squareup.moshi.Moshi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

internal class DefaultAppPreferencesStore(
    moshi: Moshi,
    dispatchers: CoroutineDispatcherProvider,
    preferencesDataStore: DataStore<Preferences>,
) : AppPreferencesStore(moshi, dispatchers), DataStore<Preferences> by preferencesDataStore