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
        val currencies = response.currencies
            .map {
                it.copy(
                    iconSmallUrl = response.imageHost?.plus(IMAGE_SMALL)?.format(it.id),
                    iconMediumUrl = response.imageHost?.plus(IMAGE_MEDIUM)?.format(it.id),
                )
            }
            .associateBy(CurrenciesResponse.Currency::code)

        dataStore.store(currencies)
    }

    private companion object {
        const val IMAGE_MEDIUM = "medium/%s.png"
        const val IMAGE_SMALL = "small/%s.png"
    }
}