package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface PredictionFundingWindow {

    fun open(userWalletId: UserWalletId)

    fun isOpen(userWalletId: UserWalletId): Flow<Boolean>

    /** Closes an open window once the balance settles or the window elapses; runs for as long as it is collected. */
    suspend fun watch(userWalletId: UserWalletId)
}