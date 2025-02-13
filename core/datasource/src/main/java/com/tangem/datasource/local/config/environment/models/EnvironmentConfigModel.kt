package com.tangem.datasource.local.config.environment.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Suppress("LongParameterList")
@JsonClass(generateAdapter = true)
class EnvironmentConfigModel(
    @Json(name = "mercuryoWidgetId") val mercuryoWidgetId: String,
    @Json(name = "mercuryoSecret") val mercuryoSecret: String,
    @Json(name = "moonPayApiKey") val moonPayApiKey: String,
    @Json(name = "moonPayApiSecretKey") val moonPayApiSecretKey: String,
    @Json(name = "blockchairApiKeys") val blockchairApiKeys: List<String>,
    @Json(name = "blockchairAuthorizationToken") val blockchairAuthorizationToken: String?,
    @Json(name = "quiknodeSubdomain") val quiknodeSubdomain: String,
    @Json(name = "quiknodeApiKey") val quiknodeApiKey: String,
    @Json(name = "bscQuiknodeSubdomain") val bscQuiknodeSubdomain: String,
    @Json(name = "bscQuiknodeApiKey") val bscQuiknodeApiKey: String,
    @Json(name = "nowNodesApiKey") val nowNodesApiKey: String,
    @Json(name = "getBlockAccessTokens") val getBlockAccessTokens: GetBlockAccessTokens?,
    @Json(name = "tonCenterApiKey") val tonCenterKeys: TonCenterKeys,
    @Json(name = "blockcypherTokens") val blockcypherTokens: Set<String>?,
    @Json(name = "infuraProjectId") val infuraProjectId: String?,
    @Json(name = "tronGridApiKey") val tronGridApiKey: String,
    @Json(name = "amplitudeApiKey") val amplitudeApiKey: String,
    @Json(name = "kaspaSecondaryApiUrl") val kaspaSecondaryApiUrl: String,
    @Json(name = "walletConnectProjectId") val walletConnectProjectId: String,
    @Json(name = "chiaFireAcademyApiKey") val chiaFireAcademyApiKey: String?,
    @Json(name = "chiaTangemApiKey") val chiaTangemApiKey: String?,
    @Json(name = "devExpress") val devExpress: ExpressModel?,
    @Json(name = "express") val express: ExpressModel?,
    @Json(name = "hederaArkhiaKey") val hederaArkhiaKey: String?,
    @Json(name = "polygonScanApiKey") val polygonScanApiKey: String?,
    @Json(name = "stakeKitApiKey") val stakeKitApiKey: String?,
    @Json(name = "bittensorDwellirKey") val bittensorDwellirApiKey: String?,
    @Json(name = "bittensorOnfinalityKey") val bittensorOnfinalityKey: String?,
    @Json(name = "koinosProApiKey") val koinosProApiKey: String?,
    @Json(name = "alephiumTangemApiKey") val alephiumTangemApiKey: String?,
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
    @Json(name = "ton") val ton: GetBlockToken?,
    @Json(name = "tron") val tron: GetBlockToken?,
    @Json(name = "cosmos-hub") val cosmos: GetBlockToken?,
    @Json(name = "near") val near: GetBlockToken?,
    @Json(name = "aptos") val aptos: GetBlockToken?,
    @Json(name = "dogecoin") val dogecoin: GetBlockToken?,
    @Json(name = "litecoin") val litecoin: GetBlockToken?,
    @Json(name = "dash") val dash: GetBlockToken?,
    @Json(name = "bitcoin") val bitcoin: GetBlockToken?,
    @Json(name = "algorand") val algorand: GetBlockToken?,
    @Json(name = "polygon-zkevm") val polygonZkevm: GetBlockToken?,
    @Json(name = "zksync") val zksync: GetBlockToken?,
    @Json(name = "base") val base: GetBlockToken?,
    @Json(name = "blast") val blast: GetBlockToken?,
    @Json(name = "filecoin") val filecoin: GetBlockToken?,
    @Json(name = "arbitrum-one") val arbitrum: GetBlockToken?,
    @Json(name = "bitcoinCash") val bitcoinCash: GetBlockToken?,
    @Json(name = "kusama") val kusama: GetBlockToken?,
    @Json(name = "moonbeam") val moonbeam: GetBlockToken?,
    @Json(name = "optimism") val optimism: GetBlockToken?,
    @Json(name = "polkadot") val polkadot: GetBlockToken?,
    @Json(name = "shibarium") val shibarium: GetBlockToken?,
    @Json(name = "sui") val sui: GetBlockToken?,
    @Json(name = "telos") val telos: GetBlockToken?,
    @Json(name = "tezos") val tezos: GetBlockToken?,
)

@JsonClass(generateAdapter = true)
data class TonCenterKeys(
    @Json(name = "mainnet") val mainnet: String,
    @Json(name = "testnet") val testnet: String,
)

@JsonClass(generateAdapter = true)
data class GetBlockToken(
    @Json(name = "jsonRpc") val jsonRPC: String?,
    @Json(name = "blockBookRest") val blockBookRest: String?,
    @Json(name = "rest") val rest: String?,
    @Json(name = "rosetta") val rosetta: String?,
)

@JsonClass(generateAdapter = true)
data class ExpressModel(
    @Json(name = "apiKey")
    val apiKey: String,
    @Json(name = "signVerifierPublicKey")
    val signVerifierPublicKey: String,
)