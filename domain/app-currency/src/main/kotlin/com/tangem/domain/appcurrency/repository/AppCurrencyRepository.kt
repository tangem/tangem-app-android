package com.tangem.domain.appcurrency.repository

import com.tangem.domain.appcurrency.model.AppCurrency
import kotlinx.coroutines.flow.Flow

interface AppCurrencyRepository {

    fun getSelectedAppCurrency(): Flow<AppCurrency>

    suspend fun getAvailableAppCurrencies(): List<AppCurrency>

    suspend fun changeAppCurrency(appCurrency: AppCurrency)
}
