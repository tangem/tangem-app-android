package com.tangem.tap.features.shop.domain.models

private const val TANGEM_WALLET_2_CARDS_SKU = "TG115X2-S"
private const val TANGEM_WALLET_3_CARDS_SKU = "TG115X3-S"

enum class ProductType(val sku: String) {

    WALLET_2_CARDS(TANGEM_WALLET_2_CARDS_SKU),
    WALLET_3_CARDS(TANGEM_WALLET_3_CARDS_SKU),
    ;

    companion object {
        val SKUS_TO_DISPLAY = listOf(TANGEM_WALLET_2_CARDS_SKU, TANGEM_WALLET_3_CARDS_SKU)
        fun fromSku(sku: String): ProductType? {
            return when (sku) {
                WALLET_2_CARDS.sku -> WALLET_2_CARDS
                WALLET_3_CARDS.sku -> WALLET_3_CARDS
                else -> null
            }
        }
    }
}