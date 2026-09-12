package com.tangem.grow.datasource.polymarket.bridge.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class PolymarketSupportedAssetsResponse(
    @Json(name = "supportedAssets") val supportedAssets: List<PolymarketSupportedAssetDto>,
)

@JsonClass(generateAdapter = true)
data class PolymarketSupportedAssetDto(
    @Json(name = "chainId") val chainId: String,
    @Json(name = "chainName") val chainName: String,
    @Json(name = "token") val token: PolymarketBridgeTokenDto,
    @Json(name = "minCheckoutUsd") val minCheckoutUsd: BigDecimal? = null,
)

@JsonClass(generateAdapter = true)
data class PolymarketBridgeTokenDto(
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "address") val address: String,
    @Json(name = "decimals") val decimals: Int,
)

@JsonClass(generateAdapter = true)
data class PolymarketDepositRequest(
    @Json(name = "address") val address: String,
)

@JsonClass(generateAdapter = true)
data class PolymarketDepositResponse(
    @Json(name = "address") val address: PolymarketBridgeAddressesDto,
)

@JsonClass(generateAdapter = true)
data class PolymarketBridgeAddressesDto(
    @Json(name = "evm") val evm: String,
    @Json(name = "svm") val svm: String? = null,
    @Json(name = "btc") val btc: String? = null,
    @Json(name = "tron") val tron: String? = null,
)