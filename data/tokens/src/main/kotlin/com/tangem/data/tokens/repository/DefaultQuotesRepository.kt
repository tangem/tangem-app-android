package com.tangem.data.tokens.repository

import arrow.core.Either
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.QuotesRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultQuotesRepository : QuotesRepository {

    override fun getQuotes(tokensIds: Set<Token.ID>, refresh: Boolean): Flow<Either<TokensError, Set<Quote>>> {
        TODO("Not yet implemented")
    }
}
