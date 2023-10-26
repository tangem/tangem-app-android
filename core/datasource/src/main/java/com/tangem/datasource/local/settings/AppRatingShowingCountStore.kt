package com.tangem.datasource.local.settings

import com.tangem.datasource.local.datastore.IntSharedPreferencesDataStore
import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import kotlinx.coroutines.flow.Flow

interface AppRatingShowingCountStore {

    fun get(): Flow<Int>

    suspend fun getSyncOrNull(): Int?

    suspend fun store(item: Int)
}

internal class DefaultAppRatingShowingCountStore(
    store: IntSharedPreferencesDataStore,
) : AppRatingShowingCountStore, KeylessDataStoreDecorator<Int>(
    wrappedDataStore = store,
    key = "showRatingDialogAtLaunchCount",
)