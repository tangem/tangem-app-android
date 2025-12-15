package com.tangem.domain.staking.model.ethpool

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * P2P.org pooled staking vault information
 * Simplified version for vault list display
 * Analogue of stakekit yield
 */
@Serializable
data class P2PEthPoolVault(
    val vaultAddress: String,
    val displayName: String,
    val apy: SerializedBigDecimal,
    val baseApy: SerializedBigDecimal,
    val capacity: SerializedBigDecimal,
    val totalAssets: SerializedBigDecimal,
    val feePercent: SerializedBigDecimal,
    val isPrivate: Boolean,
    val isGenesis: Boolean,
    val isSmoothingPool: Boolean,
    val isErc20: Boolean,
    val tokenName: String?,
    val tokenSymbol: String?,
    val createdAt: Long,
)