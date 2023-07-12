package com.tangem.domain.tokens.repository

import arrow.core.Either
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import kotlinx.coroutines.flow.Flow
// [REDACTED_TODO_COMMENT]
// [REDACTED_JIRA]

/**
 * Repository for everything related to the quotes of tokens
 * */
interface QuotesRepository {

    fun getQuotes(tokensIds: Set<Token.ID>, refresh: Boolean): Flow<Either<TokensError, Set<Quote>>>
}
