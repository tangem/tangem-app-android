package com.tangem.datasource.local.appcurrency

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import kotlinx.coroutines.flow.Flow

interface SelectedAppCurrencyStore {

    fun get(): Flow<CurrenciesResponse.Currency>

    suspend fun store(item: CurrenciesResponse.Currency)
}