package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.repositories.MarketsTokenRepository

class GetTokenFullQuotesUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {
    suspend operator fun invoke(
        appCurrency: AppCurrency,
        tokenId: String,
        tokenSymbol: String,
    ): Either<Unit, TokenQuotes> {
        return Either.catch {
            marketsTokenRepository.getTokenQuotes(
                fiatCurrencyCode = appCurrency.code,
                tokenId = tokenId,
                tokenSymbol = tokenSymbol,
            )
        }.mapLeft {}
    }
}