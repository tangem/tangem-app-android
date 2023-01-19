package com.tangem.tap.features.shop.redux

import com.tangem.tap.common.shop.data.ProductType
import com.tangem.tap.common.shop.data.TangemProduct
import org.rekotlin.StateType

data class ShopState(
    val availableProducts: List<TangemProduct> = emptyList(),
    val selectedProduct: ProductType = ProductType.WALLET_3_CARDS,
    val promoCode: String? = null,
    val promoCodeLoading: Boolean = false,
    val isGooglePayAvailable: Boolean = false, // TODO: change when we add support for GPay
) : StateType {
    val total: String?
        get() = availableProducts.firstOrNull { it.type == selectedProduct }?.totalSum?.finalValue

    val priceBeforeDiscount: String?
        get() {
            val totalSum = availableProducts.firstOrNull { it.type == selectedProduct }?.totalSum
            if (totalSum?.finalValue != totalSum?.beforeDiscount) {
                return totalSum?.beforeDiscount
            }
            return null
        }
}
