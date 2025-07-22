package com.tangem.feature.referral.domain

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.referral.domain.models.ReferralData

interface ReferralInteractor {

    suspend fun getReferralStatus(userWalletId: UserWalletId): ReferralData

    suspend fun startReferral(userWalletId: UserWalletId): ReferralData
}