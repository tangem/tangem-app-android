package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import kotlinx.coroutines.flow.Flow

// FIXME: Use Raise as context instead of Effect when context receivers become stable
// https://tangem.atlassian.net/browse/AND-3947

/**
 * Repository for everything related to the quotes of tokens
 * */
interface QuotesRepository {

    fun getQuotes(tokensIds: Set<Token.ID>, refresh: Boolean): Flow<Set<Quote>>
}
