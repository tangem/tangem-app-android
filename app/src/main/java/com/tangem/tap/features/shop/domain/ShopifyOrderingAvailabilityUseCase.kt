package com.tangem.tap.features.shop.domain

/**
 * Use case to define shopify ordering availability
 *
[REDACTED_AUTHOR]
 */
internal interface ShopifyOrderingAvailabilityUseCase {

    /** Get availability */
    suspend operator fun invoke(): Boolean
}