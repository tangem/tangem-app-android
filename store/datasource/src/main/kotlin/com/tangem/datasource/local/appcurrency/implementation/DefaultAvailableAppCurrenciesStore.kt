package com.tangem.datasource.local.appcurrency.implementation

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.appcurrency.AvailableAppCurrenciesStore

internal class DefaultAvailableAppCurrenciesStore(
    private val store: RuntimeSharedMapStore<String, CurrenciesResponse.Currency>,
) : AvailableAppCurrenciesStore {

    override suspend fun getAllSyncOrNull(): List<CurrenciesResponse.Currency>? = store.getAllSyncOrNull()

    override suspend fun getSyncOrNull(key: String): CurrenciesResponse.Currency? = store.getSyncOrNull(key)

    override suspend fun store(response: CurrenciesResponse) {
        val currencies = response.currencies
            .map { currency ->
                currency.copy(
                    iconSmallUrl = response.imageHost?.plus(IMAGE_SMALL)?.format(currency.id),
                    iconMediumUrl = response.imageHost?.plus(IMAGE_MEDIUM)?.format(currency.id),
                )
            }
            .associateBy(CurrenciesResponse.Currency::code)

        store.store(currencies)
    }

    private companion object {
        const val IMAGE_MEDIUM = "medium/%s.png"
        const val IMAGE_SMALL = "small/%s.png"
    }
}