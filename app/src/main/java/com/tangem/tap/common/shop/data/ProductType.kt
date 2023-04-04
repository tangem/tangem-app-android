package com.tangem.tap.common.shop.data

import com.tangem.tap.common.shop.TangemShopService

enum class ProductType(val sku: String) {
    WALLET_2_CARDS(TangemShopService.TANGEM_WALLET_2_CARDS_SKU),
    WALLET_3_CARDS(TangemShopService.TANGEM_WALLET_3_CARDS_SKU),
    ;

    companion object {
        fun fromSku(sku: String): ProductType? {
            return when (sku) {
                WALLET_2_CARDS.sku -> WALLET_2_CARDS
                WALLET_3_CARDS.sku -> WALLET_3_CARDS
                else -> null
            }
        }
    }
}
