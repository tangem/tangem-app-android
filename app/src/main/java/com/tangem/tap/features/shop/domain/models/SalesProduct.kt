package com.tangem.tap.features.shop.domain.models

/**
 * Sales product
 *
 * @property id product id
 * @property productType shows TW2 cards or 3 cards
 * @property state state as order available etc
 * @property name product name
 * @property notification optional notification
 */
data class SalesProduct(
    val id: String,
    val productType: ProductType,
    val state: ProductState,
    val name: String,
    val notification: Notification?,
)

data class Notification(
    val type: String,
    val title: String,
    val description: String,
)

enum class ProductState {
    ORDER,
    SOLD_OUT,
    PRE_ORDER,
}
