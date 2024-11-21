package com.tangem.datasource.local.onramp.quotes

import com.tangem.datasource.api.onramp.models.response.OnrampQuoteResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator

internal class DefaultOnrampQuotesStore(
    dataStore: StringKeyDataStore<List<OnrampQuoteResponse>>,
) : OnrampQuotesStore, StringKeyDataStoreDecorator<String, List<OnrampQuoteResponse>>(dataStore) {
    override fun provideStringKey(key: String): String = key
}
