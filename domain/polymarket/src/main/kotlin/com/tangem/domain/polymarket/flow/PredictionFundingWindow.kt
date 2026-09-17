package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface PredictionFundingWindow {

    /**
     * Records that a top-up has been sent to [userWalletId]'s prediction account, so the balance is polled
     * faster and the awaiting-funds banner is shown until the funds land or the window elapses.
     */
    fun open(userWalletId: UserWalletId)

    fun isOpen(userWalletId: UserWalletId): Flow<Boolean>

    /** Closes an open window once the balance settles or the window elapses; runs for as long as it is collected. */
    suspend fun watch(userWalletId: UserWalletId)
}