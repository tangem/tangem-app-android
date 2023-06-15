package com.tangem.tap.features.shop.domain

/**
 * Shop feature repository
 *
 * @author Andrew Khokhlov on 15/06/2023
 */
internal interface ShopRepository {

    /** Get shopify ordering availability */
    suspend fun isShopifyOrderingAvailable(): Boolean
}
