package com.tangem.data.tokens.repository

import com.tangem.data.tokens.mock.MockQuotes
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.QuotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class MockQuotesRepository : QuotesRepository {

    override fun getQuotes(tokensIds: Set<Token.ID>, refresh: Boolean): Flow<Set<Quote>> {
        return flowOf(
            MockQuotes.quotes
                .filter { it.tokenId in tokensIds }
                .toSet(),
        )
    }
}