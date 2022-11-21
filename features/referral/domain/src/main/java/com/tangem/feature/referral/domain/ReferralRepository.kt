package com.tangem.feature.referral.domain

import com.tangem.feature.referral.domain.models.ReferralData

interface ReferralRepository {

    /** Returns data object of [ReferralData] depends on user program status */
    suspend fun getReferralStatus(walletId: String): ReferralData

    /** Starts user referral program */
    suspend fun startReferral(
        walletId: String,
        networkId: String,
        tokenId: String,
        address: String,
    ): ReferralData
}
