package com.tangem.tap.features.shop.domain

/**
 * Shop feature repository
 *
[REDACTED_AUTHOR]
 */
internal interface ShopRepository {

    /** Get shopify ordering availability */
    suspend fun isShopifyOrderingAvailable(): Boolean
}