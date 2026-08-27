package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketCategory

/**
 * Serves the BFF-owned UI categories shown as Discovery feed tabs.
 */
class GetPolymarketCategoriesUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(): Either<DataError, List<PolymarketCategory>> {
        return polymarketRepository.getCategories()
    }
}