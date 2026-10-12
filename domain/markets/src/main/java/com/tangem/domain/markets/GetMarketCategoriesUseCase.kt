package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.markets.repositories.MarketsTokenRepository

/**
 * Get one page of market categories use case
 *
 * @property marketsTokenRepository markets token repository
 */
class GetMarketCategoriesUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(
        currency: String,
        offset: Int,
        limit: Int,
        timestamp: Long? = null,
        featured: Boolean? = null,
    ): Either<Throwable, MarketCategoryPage> {
        return Either.catch {
            marketsTokenRepository.getMarketCategories(
                currency = currency,
                offset = offset,
                limit = limit,
                timestamp = timestamp,
                featured = featured,
            )
        }
    }
}