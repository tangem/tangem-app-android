package com.tangem.domain.polymarket.model

import arrow.core.NonEmptyList

/**
 * The contract calls that redeem a position, as the backend served them and in the order to sign them: one
 * on a collateral adapter for a V1 market, one per held outcome on the Router for a V2 market.
 *
 * @property version the market's protocol as the backend states it; `null` when it did not say
 */
data class PredictionRedeemCalls(
    val version: PolymarketMarketVersion?,
    val calls: NonEmptyList<PolymarketApprovalCall>,
)

/**
 * A claim batch as the card signed it. [calls] are the backend's [PredictionRedeemCalls.calls] verbatim and
 * in order: the signature covers the array, and `POST /positions/redeem` accepts only calls it served itself.
 */
data class PredictionClaimBatch(
    val ownerAddress: String,
    val depositWalletAddress: String,
    val nonce: String,
    val deadline: String,
    val calls: NonEmptyList<PolymarketApprovalCall>,
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