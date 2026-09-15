package com.tangem.domain.polymarket.model

import com.tangem.domain.models.wallet.UserWalletId

/**
 * A withdrawal batch as the card signed it. The single [call] is the contract's invariant made a type:
 * the wire format is an array only because the signature covers an array, and a withdrawal is never
 * batched.
 */
data class PredictionWithdrawalBatch(
    val ownerAddress: String,
    val depositWalletAddress: String,
    val nonce: String,
    val deadline: String,
    val call: PolymarketApprovalCall,
    val signature: String,
)

/**
 * @property transactionHash the Polygon hash, nullable in every state — including `CONFIRMED`, and
 *  including `FAILED`, where it is most useful
 */
data class PredictionWithdrawalReceipt(
    val transactionId: String,
    val state: PredictionWithdrawalState,
    val transactionHash: String?,
)

/** Leg 1 only: whether pUSD has left the deposit wallet. `CONFIRMED` is not money in the user's hands. */
enum class PredictionWithdrawalState {
    IN_PROGRESS,
    CONFIRMED,
    FAILED,
    ;

    val isTerminal: Boolean
        get() = this != IN_PROGRESS

    companion object {

        /** @return [IN_PROGRESS] for anything unrecognised, so a new backend state cannot fake a terminal. */
        fun fromRaw(raw: String): PredictionWithdrawalState =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: IN_PROGRESS
    }
}

/** @property transactionId null until the BFF acknowledged the batch with a `202`. */
data class PredictionWithdrawalRecord(
    val batch: PredictionWithdrawalBatch,
    val transactionId: String?,
)

/** @property decoded the recipient and amount read out of [call], the values a confirmation screen must display */
data class PreparedPredictionWithdrawal(
    val userWalletId: UserWalletId,
    val walletId: String,
    val ownerAddress: String,
    val depositWalletAddress: String,
    val nonce: String,
    val deadline: String,
    val call: PolymarketApprovalCall,
    val decoded: PredictionTransferCall,
)