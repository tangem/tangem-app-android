package com.tangem.data.wallets

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.wallets.repository.WalletsRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultWalletsRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : WalletsRepository {

    override suspend fun shouldSaveUserWalletsSync(): Boolean {
        return appPreferencesStore.getSyncOrDefault(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, default = false)
    }

    override fun shouldSaveUserWallets(): Flow<Boolean> {
        return appPreferencesStore.get(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, default = false)
    }

    override suspend fun saveShouldSaveUserWallets(item: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SAVE_USER_WALLETS_KEY, value = item)
    }

    override suspend fun setHasWalletsWithRing() {
        appPreferencesStore.store(key = PreferencesKeys.IS_RING_ADDED_KEY, value = true)
    }
}
