package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

/**
 * Implementation of [UserTokensStore] that based on [appPreferencesStore]
 *
 * @property appPreferencesStore application preference store
 *
 * @author Andrew Khokhlov on 23/06/2024
 */
internal class AppPreferencesUserTokensStore(
    private val appPreferencesStore: AppPreferencesStore,
) : UserTokensStore {

    override fun get(key: UserWalletId): Flow<UserTokensResponse> {
        return appPreferencesStore
            .getObject<UserTokensResponse>(PreferencesKeys.getUserTokensKey(userWalletId = key.stringValue))
            .filterNotNull()
    }

    override suspend fun getSyncOrNull(key: UserWalletId): UserTokensResponse? {
        return appPreferencesStore.getObjectSyncOrNull(
            key = PreferencesKeys.getUserTokensKey(userWalletId = key.stringValue),
        )
    }

    override suspend fun store(key: UserWalletId, value: UserTokensResponse) {
        appPreferencesStore.storeObject(
            key = PreferencesKeys.getUserTokensKey(userWalletId = key.stringValue),
            value = value,
        )
    }
}
