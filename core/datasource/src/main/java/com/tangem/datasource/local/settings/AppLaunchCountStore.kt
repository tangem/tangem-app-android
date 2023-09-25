package com.tangem.datasource.local.settings

import com.tangem.datasource.local.datastore.IntSharedPreferencesDataStore
import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import kotlinx.coroutines.flow.Flow

interface AppLaunchCountStore {

    fun get(): Flow<Int>

    suspend fun getSyncOrNull(): Int?

    suspend fun store(item: Int)
}

internal class DefaultAppLaunchCountStore(
    store: IntSharedPreferencesDataStore,
) : AppLaunchCountStore, KeylessDataStoreDecorator<Int>(
    wrappedDataStore = store,
    key = "launchCount",
)