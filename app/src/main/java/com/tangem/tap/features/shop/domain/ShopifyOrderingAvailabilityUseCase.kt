package com.tangem.tap.features.shop.domain

/**
 * Use case to define shopify ordering availability
 *
 * @author Andrew Khokhlov on 15/06/2023
 */
internal interface ShopifyOrderingAvailabilityUseCase {

    /** Get availability */
    suspend operator fun invoke(): Boolean
}
