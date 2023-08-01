package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the quotes of tokens
 * */
interface QuotesRepository {

    fun getQuotes(tokensIds: Set<CryptoCurrency.ID>, refresh: Boolean): Flow<Set<Quote>>
}
