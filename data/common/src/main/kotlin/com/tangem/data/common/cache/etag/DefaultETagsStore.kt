package com.tangem.data.common.cache.etag

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.models.wallet.UserWalletId
import timber.log.Timber

/**
 * Default implementation of the [ETagsStore] interface for managing ETag values
 *
 * @property appPreferencesStore the preferences store used for saving and retrieving ETag values
 */
internal class DefaultETagsStore(
    private val appPreferencesStore: AppPreferencesStore,
) : ETagsStore {

    override suspend fun getSyncOrNull(userWalletId: UserWalletId, key: ETagsStore.Key): String? {
        val key = getAccountsETagKey(userWalletId = userWalletId, key = key)

        return appPreferencesStore.getSyncOrNull(key = key)
    }

    override suspend fun store(userWalletId: UserWalletId, key: ETagsStore.Key, value: String) {
        if (value.isBlank()) {
            Timber.e("ETag value is blank, not storing it. userWalletId: $userWalletId, key: $key")
            return
        }

        val key = getAccountsETagKey(userWalletId = userWalletId, key = key)

        appPreferencesStore.store(key = key, value = value)
    }

    private fun getAccountsETagKey(userWalletId: UserWalletId, key: ETagsStore.Key): Preferences.Key<String> {
        return stringPreferencesKey(name = "etag_${key}_${userWalletId.stringValue}")
    }
}