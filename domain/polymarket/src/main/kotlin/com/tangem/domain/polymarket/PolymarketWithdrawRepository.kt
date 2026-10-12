package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.domain.polymarket.model.PredictionWithdrawalBatch
import com.tangem.domain.polymarket.model.PredictionWithdrawalReceipt

interface PolymarketWithdrawRepository {

    /**
     * @param walletId 64 hex characters, no `0x`
     * @return the same receipt for a resubmitted identical batch — the submission is keyed on
     *  (deposit wallet, nonce)
     */
    suspend fun submit(
        walletId: String,
        batch: PredictionWithdrawalBatch,
    ): Either<PredictionWithdrawError, PredictionWithdrawalReceipt>

    suspend fun getState(
        walletId: String,
        transactionId: String,
    ): Either<PredictionWithdrawError, PredictionWithdrawalReceipt>
}