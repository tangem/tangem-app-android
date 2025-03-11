package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Get token exchanges use case
 *
 * @property marketsTokenRepository markets token repository
 */
class GetTokenExchangesUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(tokenId: CryptoCurrency.RawID): Either<Throwable, List<TokenMarketExchange>> {
        return Either.catch { marketsTokenRepository.getTokenExchanges(tokenId = tokenId) }
    }
}