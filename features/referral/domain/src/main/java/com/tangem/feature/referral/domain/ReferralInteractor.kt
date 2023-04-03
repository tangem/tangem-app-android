package com.tangem.feature.referral.domain

import com.tangem.feature.referral.domain.models.ReferralData

interface ReferralInteractor {

    val isDemoMode: Boolean

    suspend fun getReferralStatus(): ReferralData

    suspend fun startReferral(): ReferralData
}
