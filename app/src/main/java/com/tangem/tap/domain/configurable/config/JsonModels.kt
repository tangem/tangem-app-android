package com.tangem.tap.domain.configurable.config

import com.tangem.tap.common.shop.shopify.ShopifyShop

/**
 * Created by Anton Zhilenkov on 12/11/2020.
 */

class FeatureModel(
        val isWalletPayIdEnabled: Boolean,
        val isTopUpEnabled: Boolean,
        val isSendingToPayIdEnabled: Boolean,
        val isCreatingTwinCardsAllowed: Boolean,
)

class ConfigValueModel(
        val coinMarketCapKey: String,
        val moonPayApiKey: String,
        val onramperApiKey: String,
        val moonPayApiSecretKey: String,
        val blockchairApiKey: String?,
        val blockchairAuthorizationToken: String?,
        val blockcypherTokens: Set<String>?,
        val infuraProjectId: String?,
        val appsFlyerDevKey: String,
        val shopifyShop: ShopifyShop?
)

class ConfigModel(val features: FeatureModel?, val configValues: ConfigValueModel?) {
    companion object {
        fun empty(): ConfigModel = ConfigModel(null, null)
    }
}