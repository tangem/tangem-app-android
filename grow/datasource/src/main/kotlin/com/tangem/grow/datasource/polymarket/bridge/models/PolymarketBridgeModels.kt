package com.tangem.grow.datasource.polymarket.bridge.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response of `GET https://bridge.polymarket.com/supported-assets`. */
@JsonClass(generateAdapter = true)
data class PolymarketSupportedAssetsResponse(
    @Json(name = "supportedAssets") val supportedAssets: List<PolymarketSupportedAssetDto>,
)

/**
 * One chain-and-token combination the bridge accepts right now.
 *
 * @property minCheckoutUsd the minimum **deposit** for this asset in USD; it says nothing about withdrawals
 */
@JsonClass(generateAdapter = true)
data class PolymarketSupportedAssetDto(
    @Json(name = "chainId") val chainId: Long,
    @Json(name = "chainName") val chainName: String,
    @Json(name = "token") val token: PolymarketBridgeTokenDto,
    @Json(name = "minCheckoutUsd") val minCheckoutUsd: Double,
)

@JsonClass(generateAdapter = true)
data class PolymarketBridgeTokenDto(
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "address") val address: String,
    @Json(name = "decimals") val decimals: Int,
)

/**
 * Body of `POST https://bridge.polymarket.com/deposit`.
 *
 * @property address the user's deposit wallet, never the owner address
 */
@JsonClass(generateAdapter = true)
data class PolymarketDepositRequest(
    @Json(name = "address") val address: String,
)

/**
 * Addresses the bridge issues, one per chain family.
 *
 * @property evm the only field this flow consumes, and the only one guaranteed to be present
 * @property tron nullable because Polymarket's own pages disagree on whether the field is `tron` or `tvm`
 */
@JsonClass(generateAdapter = true)
data class PolymarketBridgeAddressesDto(
    @Json(name = "evm") val evm: String,
    @Json(name = "svm") val svm: String? = null,
    @Json(name = "btc") val btc: String? = null,
    @Json(name = "tron") val tron: String? = null,
)