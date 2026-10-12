package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.domain.polymarket.model.PredictionClaimBatch
import com.tangem.domain.polymarket.model.PredictionClaimReceipt

interface PolymarketRedeemRepository {

    /**
     * @param walletId 64 hex characters, no `0x`
     * @return the contract call to put in the batch verbatim — an offline lookup that can be retried freely
     */
    suspend fun getCall(
        walletId: String,
        conditionId: String,
        isNegRisk: Boolean,
    ): Either<PolymarketRedeemError, PolymarketApprovalCall>

    /**
     * @return the receipt of the accepted batch; a market that already has a claim in flight answers with
     *  that claim's receipt rather than relaying a second one
     */
    suspend fun submit(
        walletId: String,
        batch: PredictionClaimBatch,
    ): Either<PolymarketRedeemError, PredictionClaimReceipt>

    suspend fun getState(
        walletId: String,
        transactionId: String,
    ): Either<PolymarketRedeemError, PredictionClaimReceipt>
}