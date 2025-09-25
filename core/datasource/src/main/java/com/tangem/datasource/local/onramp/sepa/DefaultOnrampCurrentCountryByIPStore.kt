package com.tangem.datasource.local.onramp.sepa

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.onramp.model.OnrampCountry

internal class DefaultOnrampCurrentCountryByIPStore(
    val dataStore: StringKeyDataStore<OnrampCountry>,
) : OnrampCurrentCountryByIPStore, StringKeyDataStoreDecorator<Unit, OnrampCountry>(
    wrappedDataStore = dataStore,
) {

    override suspend fun getSyncOrNull(): OnrampCountry? {
        return getSyncOrNull(Unit)
    }

    override suspend fun store(value: OnrampCountry) {
        return store(Unit, value)
    }

    override fun provideStringKey(key: Unit) = "KEY"
}