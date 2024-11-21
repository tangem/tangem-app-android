package com.tangem.datasource.local.onramp.pairs

import com.tangem.datasource.api.onramp.models.response.model.OnrampPairDTO
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator

internal class DefaultOnrampPairsStore(
    dataStore: StringKeyDataStore<List<OnrampPairDTO>>,
) : OnrampPairsStore, StringKeyDataStoreDecorator<String, List<OnrampPairDTO>>(dataStore) {
    override fun provideStringKey(key: String): String = key
}
