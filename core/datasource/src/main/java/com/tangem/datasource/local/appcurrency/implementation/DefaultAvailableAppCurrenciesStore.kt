package com.tangem.datasource.local.appcurrency.implementation

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.appcurrency.AvailableAppCurrenciesStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator

internal class DefaultAvailableAppCurrenciesStore(
    private val dataStore: StringKeyDataStore<CurrenciesResponse.Currency>,
) : AvailableAppCurrenciesStore,
    StringKeyDataStoreDecorator<String, CurrenciesResponse.Currency>(dataStore) {

    override fun provideStringKey(key: String): String {
        return key
    }

    override suspend fun store(response: CurrenciesResponse) {
        val currencies = response.currencies.associateBy(CurrenciesResponse.Currency::code)

        dataStore.store(currencies)
    }
}