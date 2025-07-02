package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Default implementation of [UserTokensResponseStore]
 *
 * @property appPreferencesStore app preferences store
 *
[REDACTED_AUTHOR]
 */
internal class DefaultUserTokensResponseStore(
    private val appPreferencesStore: AppPreferencesStore,
) : UserTokensResponseStore {

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): UserTokensResponse? {
        return appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
            key = PreferencesKeys.getUserTokensKey(userWalletId = userWalletId.stringValue),
        )
    }
}