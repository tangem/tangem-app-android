package com.tangem.domain.models.earn

import kotlinx.serialization.Serializable

/**
 * Model for networks in Earn.
 * @param networkId - id of network
 * @param isAdded - is exists in any user's wallet
 */
@Serializable
data class EarnNetwork(
    val networkId: String,
    val isAdded: Boolean,
)