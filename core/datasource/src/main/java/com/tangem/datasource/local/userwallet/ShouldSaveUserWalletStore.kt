package com.tangem.datasource.local.userwallet

import com.tangem.datasource.local.datastore.BooleanSharedPreferencesDataStore
import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import kotlinx.coroutines.flow.Flow

/**
[REDACTED_AUTHOR]
 */
interface ShouldSaveUserWalletStore {

    fun get(): Flow<Boolean>

    suspend fun getSyncOrNull(): Boolean?

    suspend fun store(item: Boolean)
}

internal class DefaultShouldSaveUserWalletStore(
    store: BooleanSharedPreferencesDataStore,
) : ShouldSaveUserWalletStore, KeylessDataStoreDecorator<Boolean>(
    wrappedDataStore = store,
    key = "saveUserWallets",
)