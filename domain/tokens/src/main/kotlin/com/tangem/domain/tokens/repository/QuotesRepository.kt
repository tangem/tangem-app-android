package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the quotes of tokens
 * */
interface QuotesRepository {

    fun getQuotes(tokensIds: Set<Token.ID>, refresh: Boolean): Flow<Set<Quote>>
}