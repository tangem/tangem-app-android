package com.tangem.datasource.local.appcurrency.implementation

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.appcurrency.SelectedAppCurrencyStore
import com.tangem.datasource.local.datastore.core.KeylessDataStoreDecorator
import com.tangem.datasource.local.datastore.core.StringKeyDataStore

internal class DefaultSelectedAppCurrencyStore(
    dataStore: StringKeyDataStore<CurrenciesResponse.Currency>,
) : SelectedAppCurrencyStore, KeylessDataStoreDecorator<CurrenciesResponse.Currency>(dataStore)
