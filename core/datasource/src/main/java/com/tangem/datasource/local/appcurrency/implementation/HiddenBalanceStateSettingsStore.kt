package com.tangem.datasource.local.appcurrency.implementation

import com.tangem.datasource.local.appcurrency.HiddenBalanceSettingsStore
import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.balance_hiding.BalanceHidingSettings

internal class HiddenBalanceStateSettingsStore(
    dataStore: StringKeyDataStore<BalanceHidingSettings>,
) : HiddenBalanceSettingsStore, KeylessDataStoreDecorator<BalanceHidingSettings>(dataStore) {

    override suspend fun getSyncOrDefault(): BalanceHidingSettings {
        return getSyncOrNull() ?: BalanceHidingSettings(
            isHidingEnabledInSettings = false,
            isBalanceHidden = false
        )
    }
}
