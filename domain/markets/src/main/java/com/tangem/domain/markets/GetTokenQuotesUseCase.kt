package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.repositories.MarketsTokenRepository

class GetTokenQuotesUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(appCurrency: AppCurrency, tokenId: String): Either<Unit, TokenQuotes> {
        return Either.catch {
            marketsTokenRepository.getTokenQuotes(
                fiatCurrencyCode = appCurrency.code,
                tokenId = tokenId,
            )
        }.mapLeft {}
    }
}