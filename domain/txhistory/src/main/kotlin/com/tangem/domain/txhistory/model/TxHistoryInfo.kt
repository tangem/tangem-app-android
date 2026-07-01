package com.tangem.domain.txhistory.model

import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.OnrampTransaction
import com.tangem.domain.models.network.TxInfo

/**
 * A single row of the unified transaction history shown on the token-details screen.
 *
 * Two origins are merged into one timeline:
 *  - [OnChainTx] — a blockchain transaction (the pagination backbone).
 *  - [ExpressTx] — an exchange (swap) or onramp operation persisted by the express sync. It may be a
 *    standalone live row (no on-chain leg loaded yet) or enriched with its matched on-chain leg.
 */
sealed interface TxHistoryInfo {

    /**
     * Stable identity of the row: the express `txId` for [ExpressTx], the on-chain [identityKey]
     * for [OnChainTx].
     */
    val txId: String

    /** Position of the row in the timeline (ms since epoch), used for the global timestamp-DESC sort. */
    val timestampMillis: Long
}

/**
 * A blockchain transaction row. Sealed over its origin so new on-chain sources can be added without
 * touching the merge/UI seam: today only [BSDK] (the blockchain-SDK history that backs pagination);
 * future sources (e.g. TangemPay, Gateway) join as sibling subtypes.
 */
sealed interface OnChainTx : TxHistoryInfo {

    /** On-chain tx surfaced by the blockchain SDK — the pagination backbone, wrapping a raw [TxInfo]. */
    data class BSDK(val txInfo: TxInfo) : OnChainTx {
        override val txId: String get() = txInfo.identityKey()
        override val timestampMillis: Long get() = txInfo.timestampInMillis
    }

    // todo txHistory next step
    /*data class TangemPay(val txInfo: TangemPayTxHistoryItem) : OnChainTx {
        override val txId: String get() = txInfo.id
        override val timestampMillis: Long get() = txInfo.date.millis
    }*/

    // todo txHistory next step
    /*data class Gateway() : OnChainTx {
        override val txId: String get() = txInfo.id
        override val timestampMillis: Long get() = txInfo.date.millis
    }*/
}

/**
 * Cross-batch identity of a tx: `txHash` alone is not enough because gasless flows surface several
 * events under the same on-chain hash (e.g. `GaslessFee` + `Transfer`). Pinning the [TxInfo.type]
 * keeps those legitimate sibling events apart while still collapsing the same event seen twice —
 * e.g. an Unconfirmed copy injected via `addRecentTransactions` and a Confirmed copy that arrives
 * in a later API batch.
 *
 * Single source of truth for tx identity, used both by [OnChainTx.BSDK.txId] and the on-chain de-duplication
 * in the history pipeline.
 */
fun TxInfo.identityKey(): String = "$txHash|$type"

/**
 * A history row backed by an express operation. It is a thin wrapper over the standalone express
 * model ([ExchangeTransaction] / [OnrampTransaction]), adding only the history-view concerns:
 * the matched on-chain leg ([txInfo]) and, for swaps, which side the viewed currency is on
 * ([Swap.isOutgoing]). Everything intrinsic to the deal is delegated to the wrapped model.
 */
sealed interface ExpressTx : TxHistoryInfo {

    override val txId: String

    /** Creation timestamp (ms). Used as the row position while there is no matched on-chain leg. */
    val createdAtMillis: Long

    /**
     * Hash that joins this express op to its on-chain leg:
     * `payinHash` for outgoing (this currency is the swap's `from`), `payoutHash` otherwise.
     */
    val matchHash: String?

    /** Matched on-chain leg; `null` while it has not loaded yet (standalone live row). */
    val txInfo: OnChainTx?

    /** Provider behind this op, resolved from the local providers table by `providerId`; `null` if unknown. */
    val provider: ExpressProvider?

    /** Whether the deal reached a final state. Delegates to the wrapped model's typed status. */
    val isTerminal: Boolean

    override val timestampMillis: Long get() = txInfo?.timestampMillis ?: createdAtMillis

    data class Swap(
        val tx: ExchangeTransaction,
        /** Whether the viewed currency is the swap's `from` (pay-in) side. */
        val isOutgoing: Boolean,
        override val txInfo: OnChainTx?,
    ) : ExpressTx {
        override val txId: String get() = tx.txId
        override val createdAtMillis: Long get() = tx.createdAtMillis
        override val matchHash: String? get() = if (isOutgoing) tx.payinHash else tx.payoutHash
        override val provider: ExpressProvider? get() = tx.provider
        override val isTerminal: Boolean get() = tx.status.isTerminal
    }

    data class Onramp(
        val tx: OnrampTransaction,
        override val txInfo: OnChainTx?,
    ) : ExpressTx {
        override val txId: String get() = tx.txId
        override val createdAtMillis: Long get() = tx.createdAtMillis
        override val matchHash: String? get() = tx.payoutHash
        override val provider: ExpressProvider? get() = tx.provider
        override val isTerminal: Boolean get() = tx.status.isTerminal
    }
}