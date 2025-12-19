package com.tangem.domain.pay

import com.tangem.domain.models.wallet.UserWallet

interface TangemPayEligibilityManager {

    suspend fun getEligibleWallets(excludePaeraCustomers: Boolean): List<UserWallet>
}