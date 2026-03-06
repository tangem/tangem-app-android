package com.tangem.data.account.store

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * Legacy store for user tokens
 *
 * @property appPreferencesStore app preferences store
 *
[REDACTED_AUTHOR]
 */
internal class LegacyUserTokensResponseStore @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
) {

    suspend fun getSyncOrNull(userWalletId: UserWalletId): UserTokensResponse? {
        return appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
            key = createPreferencesKey(userWalletId = userWalletId.stringValue),
        )
    }

    suspend fun clear(userWalletId: UserWalletId) {
        appPreferencesStore.updateData { preferences ->
            val key = createPreferencesKey(userWalletId = userWalletId.stringValue)

            preferences.toMutablePreferences().apply { remove(key) }
        }
    }

    private fun createPreferencesKey(userWalletId: String) = stringPreferencesKey(name = "user_tokens_$userWalletId")
}