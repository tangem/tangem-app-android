package com.tangem.datasource.local.onramp.quotes

import com.tangem.domain.onramp.model.OnrampQuote
import kotlinx.coroutines.flow.Flow

interface OnrampQuotesStore {
    suspend fun getSyncOrNull(key: String): List<OnrampQuote>?
    fun get(key: String): Flow<List<OnrampQuote>>
    suspend fun store(key: String, value: List<OnrampQuote>)
    suspend fun clear()
}