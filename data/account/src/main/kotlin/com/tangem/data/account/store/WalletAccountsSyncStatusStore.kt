package com.tangem.data.account.store

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WalletAccountsSyncStatusStore @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
) {

    suspend fun isSyncedWithRemote(userWalletId: UserWalletId): Boolean {
        return appPreferencesStore.getSyncOrNull(key = localOnlyKey(userWalletId)) != true
    }

    suspend fun markLocalOnly(userWalletId: UserWalletId) {
        appPreferencesStore.store(key = localOnlyKey(userWalletId), value = true)
    }

    suspend fun markSyncedWithRemote(userWalletId: UserWalletId) {
        appPreferencesStore.editData { it.remove(localOnlyKey(userWalletId)) }
    }

    suspend fun clear(userWalletIds: List<UserWalletId>) {
        if (userWalletIds.isEmpty()) return

        appPreferencesStore.editData { preferences ->
            userWalletIds.forEach { userWalletId -> preferences.remove(localOnlyKey(userWalletId)) }
        }
    }

    private fun localOnlyKey(userWalletId: UserWalletId): Preferences.Key<Boolean> {
        return booleanPreferencesKey(name = "wallet_accounts_local_only_${userWalletId.stringValue}")
    }
}