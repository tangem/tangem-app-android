package com.tangem.features.txhistory.utils

import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo

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
            result += op.withMatchedTxInfo(matched)
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

private fun ExpressTx.withMatchedTxInfo(txInfo: TxInfo): ExpressTx {
    val matched = OnChainTx.BSDK(txInfo)
    return when (this) {
        is ExpressTx.Swap -> copy(txInfo = matched)
        is ExpressTx.Onramp -> copy(txInfo = matched)
    }
}