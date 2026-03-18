package com.tangem.feature.referral.domain

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.TokenData

interface ReferralInteractor {

    suspend fun getReferralStatus(userWalletId: UserWalletId): ReferralData

    suspend fun startReferral(accountId: AccountId): ReferralData

    suspend fun getCryptoCurrency(
        userWalletId: UserWalletId,
        tokenData: TokenData,
        accountIndex: DerivationIndex?,
    ): CryptoCurrency?
}