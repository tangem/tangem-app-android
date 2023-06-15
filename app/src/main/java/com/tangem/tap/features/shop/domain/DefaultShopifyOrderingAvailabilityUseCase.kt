package com.tangem.tap.features.shop.domain

/**
 * Default implementation of use case to define shopify ordering availability
 *
 * @property shopRepository shop feature repository
 *
 * @author Andrew Khokhlov on 15/06/2023
 */
internal class DefaultShopifyOrderingAvailabilityUseCase(
    private val shopRepository: ShopRepository,
) : ShopifyOrderingAvailabilityUseCase {

    override suspend fun invoke() = shopRepository.isShopifyOrderingAvailable()
}
