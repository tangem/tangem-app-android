package com.tangem.tap.common.shop.data

data class TangemProduct(
    val type: ProductType,
    val totalSum: TotalSum? = null,
    val appliedDiscount: String? = null
)