package com.tangem.domain.referral

import kotlinx.serialization.Serializable

/**
 * Repository for handling referral-related operations.
 *
[REDACTED_AUTHOR]
 */
interface ReferralRepository {

    /** Retrieves the referral status for a given [userWalletId] */
    suspend fun getReferralStatus(userWalletId: String): ReferralStatus
}

@Serializable
data class ReferralStatus(
    val isActive: Boolean,
    val token: Token?,
    val address: String?,
) {

    @Serializable
    data class Token(
        val networkId: String,
        val contractAddress: String?,
    )
}