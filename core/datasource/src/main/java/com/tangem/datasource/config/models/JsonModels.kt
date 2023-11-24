package com.tangem.datasource.config.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
[REDACTED_AUTHOR]
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
    @Json(name = "getBlockAccessTokens") val getBlockAccessTokens: GetBlockAccessTokens?,
    @Json(name = "tonCenterApiKey") val tonCenterKeys: TonCenterKeys,
    val blockcypherTokens: Set<String>?,
    val infuraProjectId: String?,
    val appsFlyer: AppsFlyer,
    val shopifyShop: ShopifyShop?,
    val sprinklr: SprinklrConfig?,
    val tronGridApiKey: String,
    val amplitudeApiKey: String,
    val swapReferrerAccount: SwapReferrerAccount?,
    val kaspaSecondaryApiUrl: String,
    val walletConnectProjectId: String,
    val tangemComAuthorization: String?,
    val chiaFireAcademyApiKey: String?,
    val chiaTangemApiKey: String?,
    val tangemExpressApiKey: String,
)

@JsonClass(generateAdapter = true)
data class GetBlockAccessTokens(
    @Json(name = "xrp") val xrp: GetBlockToken?,
    @Json(name = "cardano") val cardano: GetBlockToken?,
    @Json(name = "avalanche") val avalanche: GetBlockToken?,
    @Json(name = "ethereum") val eth: GetBlockToken?,
    @Json(name = "ethereumClassic") val etc: GetBlockToken?,
    @Json(name = "fantom") val fantom: GetBlockToken?,
    @Json(name = "rsk") val rsk: GetBlockToken?,
    @Json(name = "bsc") val bsc: GetBlockToken?,
    @Json(name = "polygon") val polygon: GetBlockToken?,
    @Json(name = "xdai") val gnosis: GetBlockToken?,
    @Json(name = "cronos") val cronos: GetBlockToken?,
    @Json(name = "solana") val solana: GetBlockToken?,
    @Json(name = "stellar") val stellar: GetBlockToken?,
    @Json(name = "ton") val ton: GetBlockToken?,
    @Json(name = "tron") val tron: GetBlockToken?,
    @Json(name = "cosmos-hub") val cosmos: GetBlockToken?,
    @Json(name = "near") val near: GetBlockToken?,
    @Json(name = "terra-2") val luna: GetBlockToken?,
    @Json(name = "dogecoin") val dogecoin: GetBlockToken?,
    @Json(name = "litecoin") val litecoin: GetBlockToken?,
    @Json(name = "dash") val dash: GetBlockToken?,
    @Json(name = "bitcoin") val bitcoin: GetBlockToken?,
)

@JsonClass(generateAdapter = true)
data class GetBlockToken(
    @Json(name = "jsonRpc") val jsonRPC: String?,
    @Json(name = "blockBookRest") val blockBookRest: String?,
    @Json(name = "rest") val rest: String?,
    @Json(name = "rosetta") val rosetta: String?,
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