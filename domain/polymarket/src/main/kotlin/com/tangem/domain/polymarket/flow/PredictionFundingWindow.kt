package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface PredictionFundingWindow {

    fun arm(userWalletId: UserWalletId)

    fun isOpen(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun watch(userWalletId: UserWalletId)
}