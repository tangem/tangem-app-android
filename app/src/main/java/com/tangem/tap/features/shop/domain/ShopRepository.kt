package com.tangem.tap.features.shop.domain

import com.tangem.tap.features.shop.domain.models.SalesProduct

/**
 * Shop feature repository
 *
 * @author Andrew Khokhlov on 15/06/2023
 */
interface ShopRepository {

    /** Get shopify ordering availability */
    suspend fun isShopifyOrderingAvailable(): Boolean

    /** Get actual sales product info */
    suspend fun getSalesProductInfo(): List<SalesProduct>
}
