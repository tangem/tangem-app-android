package com.tangem.data.txhistory.list

import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.model.explorerHash

/**
 * Merges the on-chain pagination backbone with the express (swap/onramp) overlay into a single
 * timestamp-DESC timeline.
 *
 * Per express op (matched to on-chain by [ExpressTx.matchHash]):
 *  - matched   → enrich: emit the express row carrying its on-chain leg; the on-chain tx(es)
 *                of that hash are collapsed into this row (not emitted standalone).
 *  - unmatched → standalone row (status shown, no on-chain leg). Both in-progress and terminal
 *                (finished/failed) express ops are shown so the user always sees their deals.
 *
 * On-chain transactions that no express op claimed pass through as [OnChainTx].
 * [onChain] is expected to be already de-duplicated (by `identityKey`) by the caller.
 */
internal fun mergeTxHistoryInfos(onChain: List<TxInfo>, express: List<ExpressTx>): List<TxHistoryInfo> {
    val onChainByHash = onChain.associateBy { it.txHash }
    val matchedHashes = mutableSetOf<String>()
    val result = mutableListOf<TxHistoryInfo>()

    express.forEach { op ->
        val matched = op.matchHash?.let(onChainByHash::get)
        if (matched != null) {
            result += op.withMatchedOnChain(OnChainTx.BSDK(matched))
            matchedHashes += matched.txHash
        } else {
            result += op
        }
    }

    onChain.forEach { tx ->
        if (tx.txHash !in matchedHashes) {
            result += OnChainTx.BSDK(tx)
        }
    }

    return result.sortedByDescending(TxHistoryInfo::timestampMillis)
}

/**
 * TangemPay counterpart of [mergeTxHistoryInfos]: merges the TangemPay on-chain backbone with the
 * express overlay. Same rules as [mergeTxHistoryInfos] — matched express ops enrich (carry their
 * on-chain leg and collapse it), unmatched ops stay standalone, unclaimed on-chain rows pass through.
 *
 * TangemPay rows are matched by [OnChainTx.explorerHash] (the item's `transactionHash`) against the
 * express op's [ExpressTx.matchHash].
 *
 * WARNING: this hash-based match has NOT been validated against real TangemPay data yet — TangemPay is
 * not wired into the history end-to-end. Re-verify the hash semantics (which field carries the on-chain
 * hash, and that it lines up with the express payin/payout hash) when TangemPay is integrated for real.
 */
internal fun mergeTangemPay(onChain: List<OnChainTx.TangemPay>, express: List<ExpressTx>): List<TxHistoryInfo> {
    val onChainByHash = onChain.mapNotNull { tx -> tx.explorerHash?.let { it to tx } }.toMap()
    val matchedHashes = mutableSetOf<String>()
    val result = mutableListOf<TxHistoryInfo>()

    express.forEach { op ->
        val matched = op.matchHash?.let(onChainByHash::get)
        if (matched != null) {
            result += op.withMatchedOnChain(matched)
            matched.explorerHash?.let(matchedHashes::add)
        } else {
            result += op
        }
    }

    onChain.forEach { tx ->
        if (tx.explorerHash !in matchedHashes) {
            result += tx
        }
    }

    return result.sortedByDescending(TxHistoryInfo::timestampMillis)
}

private fun ExpressTx.withMatchedOnChain(onChain: OnChainTx): ExpressTx = when (this) {
    is ExpressTx.Swap -> copy(txInfo = onChain)
    is ExpressTx.Onramp -> copy(txInfo = onChain)
}