package com.tangem.domain.pay.flow

import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWalletId

interface PaymentAccountStatusCache {
    suspend fun getSyncOrNull(userWalletId: UserWalletId): AccountStatus.Payment?
}