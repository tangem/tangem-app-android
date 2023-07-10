package com.tangem.datasource.config.models

import com.squareup.moshi.Json

/**
* [REDACTED_AUTHOR]
 */

class FeatureModel(
    val isTopUpEnabled: Boolean,
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
    val nowNodesApiKey: String,
    val getBlockApiKey: String,
    @Json(name = "tonCenterApiKey") val tonCenterKeys: TonCenterKeys,
    val blockcypherTokens: Set<String>?,
    val infuraProjectId: String?,
    val appsFlyer: AppsFlyer,
    val shopifyShop: ShopifyShop?,
    val zendesk: ZendeskConfig?,
    val tronGridApiKey: String,
    val amplitudeApiKey: String,
    val swapReferrerAccount: SwapReferrerAccount?,
    val kaspaSecondaryApiUrl: String,
    val walletConnectProjectId: String,
    val tangemComAuthorization: String?,
)

data class AppsFlyer(
    val appsFlyerDevKey: String,
    val appsFlyerAppID: String,
)

data class SwapReferrerAccount(
    val address: String,
    val fee: String,
)

class ConfigModel(
    val features: FeatureModel?,
    val configValues: ConfigValueModel?,
) {
    companion object {
        fun empty(): ConfigModel = ConfigModel(null, null)
    }
}
