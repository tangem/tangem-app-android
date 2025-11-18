package com.tangem.feature.referral.domain

import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData

interface ReferralInteractor {

    suspend fun getReferralStatus(userWalletId: UserWalletId): ReferralData

    suspend fun startReferral(portfolioId: PortfolioId): ReferralData

    suspend fun getCryptoCurrency(userWalletId: UserWalletId, tokenData: TokenData): CryptoCurrency?
}