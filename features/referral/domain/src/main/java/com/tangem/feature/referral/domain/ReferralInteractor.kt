package com.tangem.feature.referral.domain

import com.tangem.feature.referral.domain.models.ReferralData

interface ReferralInteractor {

    suspend fun getReferralStatus(): ReferralData

    suspend fun startReferral(): ReferralData
}
