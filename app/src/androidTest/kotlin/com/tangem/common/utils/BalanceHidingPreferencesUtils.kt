package com.tangem.common.utils

import androidx.datastore.preferences.core.MutablePreferences
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.store.datasource.preferences.StorePreferencesKeys

fun AppPreferencesStore.hideBalances(preferences: MutablePreferences) {
    preferences.setObject(
        key = StorePreferencesKeys().balanceHidingSettingsKey,
        value = BalanceHidingSettings(
            isHidingEnabledInSettings = true,
            isBalanceHidden = true,
            isBalanceHidingNotificationEnabled = false,
        ),
    )
}