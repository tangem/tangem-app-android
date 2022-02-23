package com.tangem.tap.common.shop.shopify.data

import com.shopify.graphql.support.ID

data class CheckoutItem(
    val id: ID,
    val quantity: Int
)