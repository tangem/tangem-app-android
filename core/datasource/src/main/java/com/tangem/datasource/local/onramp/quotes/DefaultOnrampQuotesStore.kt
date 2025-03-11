package com.tangem.datasource.local.onramp.quotes

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.onramp.model.OnrampQuote

internal class DefaultOnrampQuotesStore(
    dataStore: StringKeyDataStore<List<OnrampQuote>>,
) : OnrampQuotesStore, StringKeyDataStoreDecorator<String, List<OnrampQuote>>(dataStore) {
    override fun provideStringKey(key: String): String = key
}