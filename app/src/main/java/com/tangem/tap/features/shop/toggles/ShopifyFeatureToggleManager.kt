package com.tangem.tap.features.shop.toggles

/**
 * Shopify feature toggle manager that provides info about shopify toggle availability
 *
 */
interface ShopifyFeatureToggleManager {

    val isDynamicSalesProductsEnabled: Boolean
}