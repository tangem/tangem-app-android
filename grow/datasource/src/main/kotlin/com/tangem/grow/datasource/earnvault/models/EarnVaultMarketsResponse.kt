package com.tangem.grow.datasource.earnvault.models

import com.tangem.domain.models.serialization.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class EarnVaultMarketsResponse(
    @SerialName("items")
    val items: List<EarnVaultMarketDto>,
)

/** APY and fees are fractions: `0.0509` is 5.09 % */
@Serializable
data class EarnVaultMarketDto(
    @SerialName("vaultId")
    val vaultId: String,
    @SerialName("name")
    val name: String,
    @SerialName("risk")
    val risk: EarnVaultRiskDto = EarnVaultRiskDto.UNKNOWN,
    @SerialName("network")
    val network: String,
    @SerialName("chainFamily")
    val chainFamily: EarnVaultChainFamilyDto = EarnVaultChainFamilyDto.UNKNOWN,
    @SerialName("chainId")
    val chainId: Int,
    @SerialName("token")
    val token: EarnVaultTokenDto,
    @SerialName("vaultAddress")
    val vaultAddress: String,
    @SerialName("wrapNative")
    val isWrapNative: Boolean,
    @SerialName("wrappedToken")
    val wrappedToken: String?,
    @SerialName("wrapGasLimit")
    val wrapGasLimit: String? = null,
    @SerialName("unwrapGasLimit")
    val unwrapGasLimit: String? = null,
    @SerialName("gaslessDeposits")
    val isGaslessDeposits: Boolean,
    @SerialName("approvalResetRequired")
    val isApprovalResetRequired: Boolean,
    @SerialName("currentApy")
    @Serializable(with = BigDecimalSerializer::class)
    val currentApy: BigDecimal,
    @SerialName("projectionApy")
    @Serializable(with = BigDecimalSerializer::class)
    val projectionApy: BigDecimal,
    @SerialName("performanceFee")
    @Serializable(with = BigDecimalSerializer::class)
    val performanceFee: BigDecimal,
    @SerialName("managementFee")
    @Serializable(with = BigDecimalSerializer::class)
    val managementFee: BigDecimal,
    @SerialName("curator")
    val curator: EarnVaultCuratorDto,
    @SerialName("description")
    val description: String,
    @SerialName("auditReportUrl")
    val auditReportUrl: String,
    @SerialName("withdrawalHours")
    val withdrawalHours: Int,
    @SerialName("unitLockSeconds")
    val unitLockSeconds: Long,
    @SerialName("depositsEnabled")
    val isDepositsEnabled: Boolean,
    @SerialName("withdrawalsEnabled")
    val isWithdrawalsEnabled: Boolean,
    @SerialName("asOf")
    val asOf: String,
    @SerialName("stale")
    val isStale: Boolean,
)

/** [address] is `null` for the network's coin */
@Serializable
data class EarnVaultTokenDto(
    @SerialName("symbol")
    val symbol: String,
    @SerialName("address")
    val address: String?,
    @SerialName("decimals")
    val decimals: Int,
)

@Serializable
data class EarnVaultCuratorDto(
    @SerialName("name")
    val name: String,
    @SerialName("url")
    val url: String,
)

@Serializable
enum class EarnVaultRiskDto {
    @SerialName("low")
    LOW,

    @SerialName("medium")
    MEDIUM,

    @SerialName("high")
    HIGH,

    /** Not a wire value: a level this build does not know is coerced to this default by `NetworkJsonFactory` */
    UNKNOWN,
}

@Serializable
enum class EarnVaultChainFamilyDto {
    @SerialName("evm")
    EVM,

    /** Not a wire value: a family this build does not know is coerced to this default by `NetworkJsonFactory` */
    UNKNOWN,
}