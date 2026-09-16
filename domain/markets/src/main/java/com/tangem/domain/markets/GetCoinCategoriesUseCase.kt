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

    /**
     * @param country ISO 3166-1 alpha-2 code the restriction flags are evaluated for, or `null` while it is
     *                still unknown. The caller resolves it: it also decides the tab's default category, so
     *                reading it here would mean resolving the same value twice.
     */
    suspend operator fun invoke(country: String?): Either<Throwable, List<CoinCategory>> {
        return Either.catch { marketsTokenRepository.getCoinCategories(country = country) }
    }
}