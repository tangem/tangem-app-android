package com.tangem.domain.models.earn

import kotlinx.serialization.Serializable

/**
 * Model for networks in Earn.
 * @param networkId - id of network
 * @param fullName - full name of network
 * @param symbol - symbol of network
 * @param isAdded - is exists in any user's wallet
 */
@Serializable
data class EarnNetwork(
    val networkId: String,
    val fullName: String,
    val symbol: String,
    val isAdded: Boolean,
)