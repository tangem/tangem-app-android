package com.tangem.datasource.local.apptheme

import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.apptheme.model.AppThemeMode

internal class DefaultAppThemeModeStore(
    dataStore: StringKeyDataStore<AppThemeMode>,
) : AppThemeModeStore, KeylessDataStoreDecorator<AppThemeMode>(dataStore)