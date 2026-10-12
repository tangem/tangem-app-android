package com.tangem.domain.pay

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayEntryPoint

interface TangemPayEligibilityManager {

    /**
     * Compatible wallets, and only while the eligibility channel of [entryPoint] is available for the client
     */
    suspend fun getEligibleWallets(
        shouldExcludePaeraCustomers: Boolean,
        entryPoint: TangemPayEntryPoint?,
    ): List<UserWallet>

    /**
     * Returns all compatible user wallets without checking Tangem Pay eligibility
     */
    suspend fun getCompatibleWalletsIds(shouldExcludePaeraCustomers: Boolean): List<UserWalletId>

    suspend fun getTangemPayAvailability(entryPoint: TangemPayEntryPoint): Boolean

    suspend fun isPaeraCustomerForAnyWallet(entryPoint: TangemPayEntryPoint): Boolean

    fun reset()
}