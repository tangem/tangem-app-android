package com.tangem.tap.common.shop.data

import com.tangem.tap.common.shop.TangemShopService

enum class ProductType(val sku: String) {
    WALLET_2_CARDS(TangemShopService.TANGEM_WALLET_2_CARDS_SKU),
    WALLET_3_CARDS(TangemShopService.TANGEM_WALLET_3_CARDS_SKU)
}