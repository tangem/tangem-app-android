package com.tangem.datasource.local.onramp.pairs

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.onramp.model.OnrampPair

internal class DefaultOnrampPairsStore(
    dataStore: StringKeyDataStore<List<OnrampPair>>,
) : OnrampPairsStore, StringKeyDataStoreDecorator<String, List<OnrampPair>>(dataStore) {
    override fun provideStringKey(key: String): String = key
}