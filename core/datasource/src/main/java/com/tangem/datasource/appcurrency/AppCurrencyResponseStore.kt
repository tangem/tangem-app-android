package com.tangem.datasource.appcurrency

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse

/**
 * Store of app currency data model [CurrenciesResponse.Currency]
 *
[REDACTED_AUTHOR]
 */
interface AppCurrencyResponseStore {

    suspend fun getSyncOrNull(): CurrenciesResponse.Currency?
}