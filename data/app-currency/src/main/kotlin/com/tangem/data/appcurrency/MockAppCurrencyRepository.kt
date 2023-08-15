package com.tangem.data.appcurrency

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class MockAppCurrencyRepository : AppCurrencyRepository {

    private val mockAppCurrencies = listOf(AppCurrency(code = "USD", name = "US Dollar", symbol = "$"))

    override fun getSelectedAppCurrency(): Flow<AppCurrency> {
        return flowOf(mockAppCurrencies.first())
    }

    override suspend fun getAvailableAppCurrencies(): List<AppCurrency> {
        return mockAppCurrencies
    }

    override suspend fun changeAppCurrency(currencyCode: String) {
        /* no-op */
    }
}