package com.tangem.datasource.local.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.quote.model.StoredQuote
import kotlinx.coroutines.flow.Flow

interface QuotesStore {

    fun get(rawCurrenciesIds: Set<String>): Flow<Set<StoredQuote>>

    suspend fun store(response: QuotesResponse)
}