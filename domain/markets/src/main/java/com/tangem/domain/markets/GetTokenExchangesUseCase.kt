package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.markets.repositories.MarketsTokenRepository

/**
 * Get token exchanges use case
 *
 * @property marketsTokenRepository markets token repository
 */
class GetTokenExchangesUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(tokenId: String): Either<Throwable, List<TokenMarketExchange>> {
        return Either.catch { marketsTokenRepository.getTokenExchanges(tokenId = tokenId) }
    }
}