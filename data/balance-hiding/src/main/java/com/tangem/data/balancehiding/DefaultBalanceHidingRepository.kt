package com.tangem.data.balancehiding

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrDefault
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultBalanceHidingRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : BalanceHidingRepository {

    override var isUpdateEnabled: Boolean = true

    override fun getBalanceHidingSettingsFlow(): Flow<BalanceHidingSettings> {
        return appPreferencesStore.getObject(
            key = PreferencesKeys.BALANCE_HIDING_SETTINGS_KEY,
            default = DEFAULT_HIDING_SETTINGS,
        )
    }

    override suspend fun storeBalanceHidingSettings(isBalanceHidden: BalanceHidingSettings) {
        appPreferencesStore.storeObject(key = PreferencesKeys.BALANCE_HIDING_SETTINGS_KEY, value = isBalanceHidden)
    }

    override suspend fun getBalanceHidingSettings(): BalanceHidingSettings {
        return appPreferencesStore.getObjectSyncOrDefault(
            key = PreferencesKeys.BALANCE_HIDING_SETTINGS_KEY,
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