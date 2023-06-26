package com.tangem.tap.features.shop.domain

/**
 * Default implementation of use case to define shopify ordering availability
 *
 * @property shopRepository shop feature repository
 *
[REDACTED_AUTHOR]
 */
internal class DefaultShopifyOrderingAvailabilityUseCase(
    private val shopRepository: ShopRepository,
) : ShopifyOrderingAvailabilityUseCase {

    override suspend fun invoke() = shopRepository.isShopifyOrderingAvailable()
}