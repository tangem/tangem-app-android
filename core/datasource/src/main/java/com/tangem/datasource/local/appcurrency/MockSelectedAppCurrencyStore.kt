package com.tangem.datasource.local.appcurrency

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// TODO: Will be implemented in [REDACTED_TASK_KEY] task
internal class MockSelectedAppCurrencyStore : SelectedAppCurrencyStore {

    override fun get(): Flow<CurrenciesResponse.Currency> {
        return flowOf(
            CurrenciesResponse.Currency(
                id = "usd",
                code = "USD",
                name = "US Dollar",
                unit = "$",
                type = "fiat",
                rateBTC = "",
            ),
        )
    }

    override suspend fun store(item: CurrenciesResponse.Currency) {
        /* no-op */
    }
}