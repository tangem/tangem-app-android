package com.tangem.datasource.local.onramp.currencies

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.onramp.model.OnrampCurrency

internal class DefaultOnrampCurrenciesStore(
    dataStore: StringKeyDataStore<List<OnrampCurrency>>,
) : OnrampCurrenciesStore, StringKeyDataStoreDecorator<String, List<OnrampCurrency>>(dataStore) {
    override fun provideStringKey(key: String): String = key
}