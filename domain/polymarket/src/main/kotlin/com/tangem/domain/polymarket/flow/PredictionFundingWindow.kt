package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface PredictionFundingWindow {

    /**
     * Records that something is about to move [userWalletId]'s prediction balance — a top-up on its way, an
     * order that reached the exchange — so the balance is polled faster and the awaiting-funds banner is
     * shown until the balance changes or the window elapses.
     */
    fun open(userWalletId: UserWalletId)

    fun isOpen(userWalletId: UserWalletId): Flow<Boolean>

    /** Closes an open window once the balance settles or the window elapses; runs for as long as it is collected. */
    suspend fun watch(userWalletId: UserWalletId)
}