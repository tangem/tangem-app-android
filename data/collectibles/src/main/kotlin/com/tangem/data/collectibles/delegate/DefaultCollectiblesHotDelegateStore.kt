package com.tangem.data.collectibles.delegate

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.data.collectibles.CollectiblesPreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.models.account.AccountId
import javax.inject.Inject

internal class DefaultCollectiblesHotDelegateStore @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
) : CollectiblesHotDelegateStore {

    override suspend fun get(accountId: AccountId): CollectiblesHotDelegateRecord? =
        appPreferencesStore.getObjectSyncOrNull(CollectiblesPreferencesKeys.getHotDelegateKey(accountId))

    override suspend fun put(accountId: AccountId, record: CollectiblesHotDelegateRecord) {
        appPreferencesStore.storeObject(CollectiblesPreferencesKeys.getHotDelegateKey(accountId), record)
    }

    override suspend fun remove(accountId: AccountId) {
        appPreferencesStore.editData { it.remove(CollectiblesPreferencesKeys.getHotDelegateKey(accountId)) }
    }
}