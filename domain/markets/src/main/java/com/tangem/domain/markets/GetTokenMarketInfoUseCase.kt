package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.repositories.MarketsTokenRepository

class GetTokenMarketInfoUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(appCurrency: AppCurrency, tokenId: String): Either<Unit, TokenMarketInfo> {
        return Either.catch {
            marketsTokenRepository.getTokenInfo(
                fiatCurrencyCode = appCurrency.code,
                tokenId = tokenId,
            )
        }.mapLeft {}
    }
}
