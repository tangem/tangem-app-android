package com.tangem.data.balancehiding

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrDefault
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultBalanceHidingRepository @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
) : BalanceHidingRepository {

    override var isUpdateEnabled: Boolean = true

    override fun getBalanceHidingSettingsFlow(): Flow<BalanceHidingSettings> {
        return appPreferencesStore.getObject(
            key = storePreferencesKeys.balanceHidingSettingsKey,
            default = DEFAULT_HIDING_SETTINGS,
        )
    }

    override suspend fun storeBalanceHidingSettings(isBalanceHidden: BalanceHidingSettings) {
        appPreferencesStore.storeObject(key = storePreferencesKeys.balanceHidingSettingsKey, value = isBalanceHidden)
    }

    override suspend fun getBalanceHidingSettings(): BalanceHidingSettings {
        return appPreferencesStore.getObjectSyncOrDefault(
            key = storePreferencesKeys.balanceHidingSettingsKey,
            default = DEFAULT_HIDING_SETTINGS,
        )
    }

    private companion object {
        val DEFAULT_HIDING_SETTINGS = BalanceHidingSettings(
            isHidingEnabledInSettings = false,
            isBalanceHidden = false,
            isBalanceHidingNotificationEnabled = true,
        )
    }
}