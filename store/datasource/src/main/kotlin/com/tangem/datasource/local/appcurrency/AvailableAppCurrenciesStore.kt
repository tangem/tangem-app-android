package com.tangem.datasource.local.appcurrency

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse

interface AvailableAppCurrenciesStore {

    suspend fun getAllSyncOrNull(): List<CurrenciesResponse.Currency>?

    suspend fun getSyncOrNull(key: String): CurrenciesResponse.Currency?

    suspend fun store(response: CurrenciesResponse)
}