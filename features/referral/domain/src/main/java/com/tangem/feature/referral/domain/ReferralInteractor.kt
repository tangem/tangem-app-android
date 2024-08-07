package com.tangem.feature.referral.domain

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.referral.domain.models.ReferralData

interface ReferralInteractor {

    val isDemoMode: Boolean

    suspend fun getReferralStatus(userWalletId: UserWalletId): ReferralData

    suspend fun startReferral(userWalletId: UserWalletId): ReferralData
}
