package com.tangem.domain.polymarket.model

/**
 * A claim batch as the card signed it. The single [call] is the contract's invariant made a type: the wire
 * format is an array only because the signature covers an array, and `POST /positions/redeem` rejects a
 * batch that does not hold exactly one redeem call.
 */
data class PredictionClaimBatch(
    val ownerAddress: String,
    val depositWalletAddress: String,
    val nonce: String,
    val deadline: String,
    val call: PolymarketApprovalCall,
    val signature: String,
)

/**
 * @property transactionHash the Polygon hash, null until there is one and absent on most failures — a
 *  display extra, never the completion signal
 */
data class PredictionClaimReceipt(
    val transactionId: String,
    val state: PredictionClaimState,
    val transactionHash: String?,
)

enum class PredictionClaimState {
    IN_PROGRESS,
    CONFIRMED,
    FAILED,
    ;

    val isTerminal: Boolean
        get() = this != IN_PROGRESS

    companion object {

        /** @return [IN_PROGRESS] for anything unrecognised, so a new backend state cannot fake a terminal. */
        fun fromRaw(raw: String): PredictionClaimState =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: IN_PROGRESS
    }
}