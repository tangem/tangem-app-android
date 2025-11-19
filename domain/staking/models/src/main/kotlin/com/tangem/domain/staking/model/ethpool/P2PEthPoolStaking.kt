package com.tangem.domain.staking.model.ethpool

import com.tangem.domain.models.serialization.SerializedBigDecimal

/**
 * P2P.org pooled staking information (similar to StakeKit's Yield)
 * Contains vault details, APY, status, and metadata
 */
data class P2PEthPoolStaking(
    val id: String, // Vault address used as unique ID
    val vault: P2PEthPoolVaultDetails,
    val status: Status,
    val apy: SerializedBigDecimal,
    val metadata: Metadata,
    val isAvailable: Boolean,
    val network: P2PEthPoolNetwork,
) {

    data class Status(
        val enter: Boolean, // Can deposit
        val exit: Boolean, // Can unstake/withdraw
    )

    data class Metadata(
        val name: String,
        val description: String?,
        val logoUri: String?,
        val documentation: String?,
        val cooldownPeriod: Period?, // Exit queue waiting time
        val warmupPeriod: Period?, // Time before rewards start
        val minimumStake: SerializedBigDecimal?,
        val maximumStake: SerializedBigDecimal?,
        val fee: Fee,
        val rewardSchedule: RewardSchedule,
        val rewardClaiming: RewardClaiming,
    ) {

        data class Period(
            val days: Int,
        )

        data class Fee(
            val enabled: Boolean,
            val percent: SerializedBigDecimal?,
        )

        enum class RewardSchedule {
            DAILY,
            WEEKLY,
            MONTHLY,
            CONTINUOUS,
            UNKNOWN,
        }

        enum class RewardClaiming {
            AUTO, // Automatically compounded
            MANUAL, // Need to claim manually
            UNKNOWN,
        }
    }
}

/**
 * Detailed vault information for P2P staking
 */
data class P2PEthPoolVaultDetails(
    val vaultAddress: String,
    val displayName: String,
    val tokenSymbol: String,
    val capacity: SerializedBigDecimal,
    val totalAssets: SerializedBigDecimal,
    val isPrivate: Boolean,
    val isGenesis: Boolean,
    val isSmoothingPool: Boolean,
    val isErc20: Boolean,
    val createdAt: String?,
)