package com.tangem.domain.polymarket.flow

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId

interface PredictionBalanceRefresher {

    suspend fun refresh(userWalletId: UserWalletId): Either<Throwable, Unit>
}