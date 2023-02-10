package com.tangem.datasource.config.models

/**
[REDACTED_AUTHOR]
 */

class FeatureModel(
    val isWalletPayIdEnabled: Boolean,
    val isTopUpEnabled: Boolean,
    val isSendingToPayIdEnabled: Boolean,
    val isCreatingTwinCardsAllowed: Boolean,
)

@Suppress("LongParameterList")
class ConfigValueModel(
    val coinMarketCapKey: String,
    val mercuryoWidgetId: String,
    val mercuryoSecret: String,
    val moonPayApiKey: String,
    val moonPayApiSecretKey: String,
    val blockchairApiKeys: List<String>,
    val blockchairAuthorizationToken: String?,
    val quiknodeSubdomain: String,
    val quiknodeApiKey: String,
    val bscQuiknodeSubdomain: String,
    val bscQuiknodeApiKey: String,
    val blockcypherTokens: Set<String>?,
    val infuraProjectId: String?,
    val appsFlyer: AppsFlyer,
    val shopifyShop: ShopifyShop?,
    val zendesk: ZendeskConfig?,
    val saltPay: SaltPayConfig,
    val tronGridApiKey: String,
    val amplitudeApiKey: String,
)

data class AppsFlyer(
    val appsFlyerDevKey: String,
    val appsFlyerAppID: String,
)

class ConfigModel(
    val features: FeatureModel?,
    val configValues: ConfigValueModel?,
) {
    companion object {
        fun empty(): ConfigModel = ConfigModel(null, null)
    }
}