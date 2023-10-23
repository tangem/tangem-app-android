package com.tangem.datasource.local.settings

import com.tangem.datasource.local.datastore.LongSharedPreferencesDataStore
import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import kotlinx.coroutines.flow.Flow

interface FundsFoundDateInMillisStore {

    fun get(): Flow<Long>

    suspend fun getSyncOrNull(): Long?

    suspend fun store(item: Long)
}

internal class DefaultFundsFoundDateInMillisStore(
    store: LongSharedPreferencesDataStore,
) : FundsFoundDateInMillisStore, KeylessDataStoreDecorator<Long>(
    wrappedDataStore = store,
    key = "fundsFoundDate",
)