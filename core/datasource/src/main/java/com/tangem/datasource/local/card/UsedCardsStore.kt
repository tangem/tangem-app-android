package com.tangem.datasource.local.card

import com.tangem.datasource.local.datastore.SharedPreferencesDataStore
import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import kotlinx.coroutines.flow.Flow

interface UsedCardsStore {

    fun get(): Flow<List<UsedCardInfo>>

    suspend fun getSyncOrNull(): List<UsedCardInfo>?

    suspend fun store(item: List<UsedCardInfo>)
}

internal class DefaultUsedCardsStore(
    store: SharedPreferencesDataStore<List<UsedCardInfo>>,
) : UsedCardsStore, KeylessDataStoreDecorator<List<UsedCardInfo>>(
    wrappedDataStore = store,
    key = "usedCardsInfo_v2",
)