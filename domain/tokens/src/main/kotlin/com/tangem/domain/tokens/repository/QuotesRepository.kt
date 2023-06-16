package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token

interface QuotesRepository {

    @Suppress("unused") // TODO
    suspend fun getQuotes(tokens: Set<Token.ID>, refresh: Boolean): Set<Quote>
}
