package com.tangem.data.wallets

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrDefault
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository

class DefaultWalletNamesMigrationRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : WalletNamesMigrationRepository {

    override suspend fun isMigrationDone(): Boolean {
        return appPreferencesStore.getObjectSyncOrDefault(
            key = PreferencesKeys.BALANCE_HIDING_SETTINGS_KEY,
            default = false,
        )
    }

    override suspend fun setMigrationDone() {
        appPreferencesStore.storeObject(PreferencesKeys.BALANCE_HIDING_SETTINGS_KEY, true)
    }
}
