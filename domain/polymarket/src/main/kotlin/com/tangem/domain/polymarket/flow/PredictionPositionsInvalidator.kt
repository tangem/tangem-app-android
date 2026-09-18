package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Announces that a wallet's positions changed on the backend and every reader must re-fetch. */
class PredictionPositionsInvalidator {

    private val events = MutableSharedFlow<UserWalletId>(extraBufferCapacity = 1)

    val signal: Flow<UserWalletId> = events.asSharedFlow()

    fun invalidate(userWalletId: UserWalletId) {
        events.tryEmit(userWalletId)
    }
}