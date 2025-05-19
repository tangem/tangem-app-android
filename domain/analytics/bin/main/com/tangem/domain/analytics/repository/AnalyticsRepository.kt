package com.tangem.domain.analytics.repository

import com.tangem.domain.analytics.model.WalletBalanceState
import com.tangem.domain.wallets.models.UserWalletId

interface AnalyticsRepository {

    suspend fun checkIsEventSent(eventId: String): Boolean

    suspend fun setIsEventSent(eventId: String)

    suspend fun getWalletBalanceState(userWalletId: UserWalletId): WalletBalanceState?

    suspend fun setWalletBalanceState(userWalletId: UserWalletId, balanceState: WalletBalanceState)
}