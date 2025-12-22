package com.tangem.domain.pay

import com.tangem.domain.models.wallet.UserWallet

interface TangemPayEligibilityManager {

    suspend fun getEligibleWallets(shouldExcludePaeraCustomers: Boolean): List<UserWallet>
}