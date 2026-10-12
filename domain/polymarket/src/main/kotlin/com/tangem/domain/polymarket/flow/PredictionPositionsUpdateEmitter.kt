package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId

/** Announces that a wallet's positions changed on the backend and every reader must re-fetch. */
interface PredictionPositionsUpdateEmitter {

    fun triggerUpdate(userWalletId: UserWalletId)
}