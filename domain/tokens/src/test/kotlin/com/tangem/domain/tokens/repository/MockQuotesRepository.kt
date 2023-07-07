package com.tangem.domain.tokens.repository

import arrow.core.Either
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import kotlinx.coroutines.flow.Flow

internal class MockQuotesRepository(
    private val quotes: Flow<Either<TokensError, Set<Quote>>>,
) : QuotesRepository {

    override fun getQuotes(tokensIds: Set<Token.ID>, refresh: Boolean): Flow<Either<TokensError, Set<Quote>>> {
        return quotes
    }
}