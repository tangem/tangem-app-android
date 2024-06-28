package com.tangem.data.wallets

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository

class DefaultWalletNamesMigrationRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : WalletNamesMigrationRepository {

    override suspend fun isMigrationDone(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.IS_WALLET_NAMES_MIGRATION_DONE_KEY,
            default = false,
        )
    }

    override suspend fun setMigrationDone() {
        appPreferencesStore.store(PreferencesKeys.IS_WALLET_NAMES_MIGRATION_DONE_KEY, true)
    }
}