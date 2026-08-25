package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.markets.repositories.MarketsTokenRepository

/**
 * Get coin categories use case
 *
 * @property marketsTokenRepository markets token repository
 */
class GetCoinCategoriesUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(): Either<Throwable, List<CoinCategory>> {
        return Either.catch { marketsTokenRepository.getCoinCategories() }
    }
}