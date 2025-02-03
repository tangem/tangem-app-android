package com.tangem.datasource.local.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.quote.model.StoredQuote
import com.tangem.domain.tokens.model.CryptoCurrency
import kotlinx.coroutines.flow.Flow

interface QuotesStore {

    fun get(currenciesIds: Set<CryptoCurrency.RawID>): Flow<Set<StoredQuote>>

    suspend fun getSync(currenciesIds: Set<CryptoCurrency.RawID>): Set<StoredQuote>

    suspend fun store(response: QuotesResponse)
}