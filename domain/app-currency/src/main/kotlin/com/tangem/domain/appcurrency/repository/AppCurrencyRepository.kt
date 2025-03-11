package com.tangem.domain.appcurrency.repository

import com.tangem.domain.appcurrency.model.AppCurrency
import kotlinx.coroutines.flow.Flow

interface AppCurrencyRepository {

    /**
     * Get Selected App Currency from cache or load from network.
     */
    fun getSelectedAppCurrency(): Flow<AppCurrency>

    suspend fun getAvailableAppCurrencies(): List<AppCurrency>

    suspend fun changeAppCurrency(currencyCode: String)

    suspend fun fetchDefaultAppCurrency(isRefresh: Boolean = false)
}