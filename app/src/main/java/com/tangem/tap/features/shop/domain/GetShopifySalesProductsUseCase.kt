package com.tangem.tap.features.shop.domain

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.tap.features.shop.domain.models.SalesError
import com.tangem.tap.features.shop.domain.models.SalesProduct

/**
 * Use case to get shopify available products
 *
 * @property shopRepository shop feature repository
 */
class GetShopifySalesProductsUseCase(
    private val shopRepository: ShopRepository,
) {

    suspend operator fun invoke(): Either<SalesError, List<SalesProduct>> {
        return try {
            shopRepository.getSalesProductInfo().right()
        } catch (e: Exception) {
            SalesError.DataError(e).left()
        }
    }
}
