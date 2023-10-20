package com.tangem.datasource.local.settings

import com.tangem.datasource.local.datastore.BooleanSharedPreferencesDataStore
import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import kotlinx.coroutines.flow.Flow

interface UserInteractingStatusStore {

    fun get(): Flow<Boolean>

    suspend fun getSyncOrNull(): Boolean?

    suspend fun store(item: Boolean)
}

internal class DefaultUserInteractingStatusStore(
    store: BooleanSharedPreferencesDataStore,
) : UserInteractingStatusStore, KeylessDataStoreDecorator<Boolean>(
    wrappedDataStore = store,
    key = "userWasInteractWithRating",
)