package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.tokens.model.CryptoCurrency

class GetTokenFullQuotesUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {
    suspend operator fun invoke(tokenId: CryptoCurrency.RawID): Either<Unit, TokenQuotesFull> {
        return Either.catch {
            marketsTokenRepository.getTokenQuotes(tokenId = tokenId)
        }.mapLeft {}
    }
}