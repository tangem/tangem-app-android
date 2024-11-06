package com.tangem.domain.onramp.repositories

import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import kotlinx.coroutines.flow.Flow

interface OnrampRepository {
    suspend fun getCurrencies(): List<OnrampCurrency>
    suspend fun getCountries(): List<OnrampCountry>
    suspend fun saveDefaultCurrency(currency: OnrampCurrency)
    suspend fun getDefaultCurrencySync(): OnrampCurrency?
    fun getDefaultCurrency(): Flow<OnrampCurrency?>
}
