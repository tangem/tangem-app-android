package com.tangem.common.utils

import androidx.datastore.preferences.core.MutablePreferences
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import kotlinx.coroutines.runBlocking

fun AppPreferencesStore.hideBalances(preferences: MutablePreferences) {
    setBalanceHidden(preferences, isBalanceHidden = true)
}

fun AppPreferencesStore.showBalances() {
    runBlocking { editData { setBalanceHidden(it, isBalanceHidden = false) } }
}

private fun AppPreferencesStore.setBalanceHidden(preferences: MutablePreferences, isBalanceHidden: Boolean) {
    preferences.setObject(
        key = StorePreferencesKeys().balanceHidingSettingsKey,
        value = BalanceHidingSettings(
            isHidingEnabledInSettings = true,
            isBalanceHidden = isBalanceHidden,
            isBalanceHidingNotificationEnabled = false,
        ),
    )
}