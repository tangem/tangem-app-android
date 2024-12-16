package com.tangem.datasource.local.onramp.currencies

import com.tangem.domain.onramp.model.OnrampCurrency
import kotlinx.coroutines.flow.Flow

interface OnrampCurrenciesStore {
    suspend fun getSyncOrNull(key: String): List<OnrampCurrency>?
    fun get(key: String): Flow<List<OnrampCurrency>>
    suspend fun store(key: String, value: List<OnrampCurrency>)
    suspend fun clear()
}