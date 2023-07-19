package com.tangem.tap.common.shop.data

import com.tangem.tap.features.shop.domain.models.ProductType

data class TangemProduct(
    val type: ProductType,
    val totalSum: TotalSum? = null,
    val appliedDiscount: String? = null,
)
