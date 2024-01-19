package com.tangem.feature.referral.domain

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData

interface ReferralRepository {

    val isDemoMode: Boolean

    /** Returns data object of [ReferralData] depends on user program status */
    suspend fun getReferralData(walletId: String): ReferralData

    /** Starts user referral program */
    suspend fun startReferral(walletId: String, networkId: String, tokenId: String, address: String): ReferralData

    suspend fun getCryptoCurrency(userWalletId: UserWalletId, tokenData: TokenData): CryptoCurrency?
}
