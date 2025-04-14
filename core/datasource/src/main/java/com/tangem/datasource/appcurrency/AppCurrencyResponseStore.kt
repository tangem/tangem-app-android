package com.tangem.datasource.appcurrency

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import kotlinx.coroutines.flow.Flow

/**
 * Store of app currency data model [CurrenciesResponse.Currency]
 *
[REDACTED_AUTHOR]
 */
interface AppCurrencyResponseStore {

    /** Get flow of [CurrenciesResponse.Currency] */
    fun get(): Flow<CurrenciesResponse.Currency?>

    /** Get [CurrenciesResponse.Currency] synchronously or null */
    suspend fun getSyncOrNull(): CurrenciesResponse.Currency?
}