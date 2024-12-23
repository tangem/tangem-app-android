package com.tangem.blockchainsdk.datastorage

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getSyncOrNull

/**
 * [BlockchainDataStorage] implementation
 *
 * @property appPreferencesStore app preferences store
 *
[REDACTED_AUTHOR]
 */
internal class DefaultBlockchainDataStorage(
    private val appPreferencesStore: AppPreferencesStore,
) : BlockchainDataStorage {

    override suspend fun getOrNull(key: String): String? {
        return appPreferencesStore.getSyncOrNull(key = stringPreferencesKey(name = key))
    }

    override suspend fun store(key: String, value: String) {
        appPreferencesStore.edit {
            it[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun remove(key: String) {
        appPreferencesStore.edit {
            it.remove(stringPreferencesKey(key))
        }
    }
}