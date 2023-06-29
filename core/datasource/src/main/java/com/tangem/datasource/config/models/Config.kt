package com.tangem.datasource.config.models

import com.tangem.blockchain.common.BlockchainSdkConfig

data class Config(
    val coinMarketCapKey: String = "f6622117-c043-47a0-8975-9d673ce484de",
    val moonPayApiKey: String = "pk_test_kc90oYTANy7UQdBavDKGfL4K9l6VEPE",
    val moonPayApiSecretKey: String = "sk_test_V8w4M19LbDjjYOt170s0tGuvXAgyEb1C",
    val mercuryoWidgetId: String = "",
    val mercuryoSecret: String = "",
    val appsFlyerDevKey: String = "",
    val amplitudeApiKey: String = "",
    val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig(),
    val isTopUpEnabled: Boolean = false,
    @Deprecated("Not relevant since version 3.23")
    val isCreatingTwinCardsAllowed: Boolean = false,
    val shopify: ShopifyShop? = null,
    val zendesk: ZendeskConfig? = null,
    val swapReferrerAccount: SwapReferrerAccount? = null,
    val walletConnectProjectId: String = "",
    val tangemComAuthorization: String? = null,
)