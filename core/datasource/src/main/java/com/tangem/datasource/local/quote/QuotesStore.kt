package com.tangem.datasource.local.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuotesStore {

    fun get(currenciesIds: Set<CryptoCurrency.RawID>): Flow<Set<Quote>>

    suspend fun getSync(currenciesIds: Set<CryptoCurrency.RawID>): Set<Quote>

    suspend fun store(response: QuotesResponse)
}