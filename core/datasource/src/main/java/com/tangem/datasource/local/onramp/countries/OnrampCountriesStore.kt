package com.tangem.datasource.local.onramp.countries

import com.tangem.domain.onramp.model.OnrampCountry
import kotlinx.coroutines.flow.Flow

interface OnrampCountriesStore {
    suspend fun getSyncOrNull(key: String): List<OnrampCountry>?
    fun get(key: String): Flow<List<OnrampCountry>>
    suspend fun store(key: String, value: List<OnrampCountry>)
    suspend fun clear()
}