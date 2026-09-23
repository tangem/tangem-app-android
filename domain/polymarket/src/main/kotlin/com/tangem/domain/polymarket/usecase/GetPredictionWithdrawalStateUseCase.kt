package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketWithdrawRepository
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.domain.polymarket.model.PredictionWithdrawalReceipt

class GetPredictionWithdrawalStateUseCase(
    private val repository: PolymarketWithdrawRepository,
) {

    suspend operator fun invoke(
        walletId: String,
        transactionId: String,
    ): Either<PredictionWithdrawError, PredictionWithdrawalReceipt> =
        repository.getState(walletId = walletId, transactionId = transactionId)
}