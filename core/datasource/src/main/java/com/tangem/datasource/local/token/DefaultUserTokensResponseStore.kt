package com.tangem.datasource.local.token

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

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

    override fun get(userWalletId: UserWalletId): Flow<UserTokensResponse?> {
        return appPreferencesStore.getObject<UserTokensResponse>(
            key = createPreferencesKey(userWalletId = userWalletId.stringValue),
        )
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): UserTokensResponse? {
        return appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
            key = createPreferencesKey(userWalletId = userWalletId.stringValue),
        )
    }

    override suspend fun store(userWalletId: UserWalletId, response: UserTokensResponse) {
        appPreferencesStore.storeObject(
            key = createPreferencesKey(userWalletId = userWalletId.stringValue),
            value = response,
        )
    }

    private fun createPreferencesKey(userWalletId: String) = stringPreferencesKey(name = "user_tokens_$userWalletId")
}