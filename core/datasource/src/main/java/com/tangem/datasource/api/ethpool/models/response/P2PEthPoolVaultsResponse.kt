package com.tangem.datasource.api.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response for GET /api/v1/staking/pool/{network}/vaults
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolVaultsResponse(
    @Json(name = "network")
    val network: P2PEthPoolNetworkDTO,
    @Json(name = "vaults")
    val vaults: List<P2PEthPoolVaultDTO>,
)

/**
 * Network identifier in P2PEthPool API
 */
@JsonClass(generateAdapter = false)
enum class P2PEthPoolNetworkDTO {
    @Json(name = "mainnet")
    MAINNET,

    @Json(name = "hoodi")
    HOODI,
}

@JsonClass(generateAdapter = true)
data class P2PEthPoolVaultDTO(
    @Json(name = "vaultAddress")
    val vaultAddress: String,
    @Json(name = "displayName")
    val displayName: String,
    @Json(name = "apy")
    val apy: BigDecimal,
    @Json(name = "baseApy")
    val baseApy: BigDecimal,
    @Json(name = "capacity")
    val capacity: BigDecimal,
    @Json(name = "totalAssets")
    val totalAssets: BigDecimal,
    @Json(name = "feePercent")
    val feePercent: BigDecimal,
    @Json(name = "isPrivate")
    val isPrivate: Boolean,
    @Json(name = "isGenesis")
    val isGenesis: Boolean,
    @Json(name = "isSmoothingPool")
    val isSmoothingPool: Boolean,
    @Json(name = "isErc20")
    val isErc20: Boolean,
    @Json(name = "tokenName")
    val tokenName: String?,
    @Json(name = "tokenSymbol")
    val tokenSymbol: String?,
    @Json(name = "createdAt")
    val createdAt: Long,
)