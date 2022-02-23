package com.tangem.tap.common.shop.shopify

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShopifyShop(
    val domain: String,
    val storefrontApiKey: String,
    val merchantID: String,
)