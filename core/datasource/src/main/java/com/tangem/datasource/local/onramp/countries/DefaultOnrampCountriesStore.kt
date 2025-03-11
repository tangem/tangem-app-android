package com.tangem.datasource.local.onramp.countries

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.onramp.model.OnrampCountry

internal class DefaultOnrampCountriesStore(
    dataStore: StringKeyDataStore<List<OnrampCountry>>,
) : OnrampCountriesStore, StringKeyDataStoreDecorator<String, List<OnrampCountry>>(dataStore) {
    override fun provideStringKey(key: String): String = key
}