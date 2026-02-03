package com.tangem.domain.models.earn

import kotlinx.serialization.Serializable

/**
 * Model of earn token.
 * @param apy - percent of earning
 * @param networkId - id of network
 * @param rewardType - apr or apy
 * @param type - staking or yield
 * @param tokenSymbol - symbol of token (ex. "ATOM")
 * @param tokenName - name of token (ex. "Cosmos Hub")
 * @param tokenId - id of token (ex. "cosmos")
 * @param tokenAddress - address of contract
 */
@Serializable
data class EarnToken(
    val apy: String,
    val networkId: String,
    val rewardType: String,
    val type: String,
    val tokenId: String,
    val tokenSymbol: String,
    val tokenName: String,
    val tokenAddress: String,
)