package com.tangem.data.wallets

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository

class DefaultWalletNamesMigrationRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
) : WalletNamesMigrationRepository {

    override suspend fun isMigrationDone(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.isWalletNamesMigrationDoneKey,
            default = false,
        )
    }

    override suspend fun setMigrationDone() {
        appPreferencesStore.store(storePreferencesKeys.isWalletNamesMigrationDoneKey, true)
    }
}