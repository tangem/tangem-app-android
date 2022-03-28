package com.tangem.tap.common.shop.shopify

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShopifyShop(
    val domain: String,
    @Json(name = "storefrontApiKeyAndroid")
    val storefrontApiKey: String,
    val merchantID: String,
)