package com.tangem.datasource.local.appcurrency.implementation

import com.tangem.datasource.local.appcurrency.BalanceHidingSettingsStore
import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.balancehiding.BalanceHidingSettings

internal class BalanceStateHidingSettingsStore(
    dataStore: StringKeyDataStore<BalanceHidingSettings>,
) : BalanceHidingSettingsStore, KeylessDataStoreDecorator<BalanceHidingSettings>(dataStore) {

    override suspend fun getSyncOrDefault(): BalanceHidingSettings {
        return getSyncOrNull() ?: BalanceHidingSettings(
            isHidingEnabledInSettings = false,
            isBalanceHidden = false,
            isBalanceHidingNotificationEnabled = true,
        )
    }
}