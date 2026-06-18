package com.tangem.features.txhistory.utils

import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
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

/**
 * Synthesizes a [TxInfo] view of an express op so it can be rendered by the existing
 * [com.tangem.features.txhistory.converter.TxHistoryItemToTransactionItemUMConverter]. Rendered as a
 * [TxInfo.TransactionType.Swap] for now (onramp included). The amount is the viewed-currency leg.
 */
internal fun ExpressTx.toSyntheticTxInfo(): TxInfo {
    val viewedAmount = when (this) {
        is ExpressTx.Swap -> if (isOutgoing) tx.fromAsset.amount else tx.toAsset.amount
        is ExpressTx.Onramp -> tx.toAsset.amount
    }
    val isOutgoing = when (this) {
        is ExpressTx.Swap -> this.isOutgoing
        is ExpressTx.Onramp -> false
    }
    return TxInfo(
        // matchHash is the on-chain hash (== the matched leg's hash, enables the explorer link); else txId.
        txHash = matchHash ?: txId,
        timestampInMillis = timestampMillis,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User(address = "")),
        sourceType = TxInfo.SourceType.Single(address = ""),
        interactionAddressType = null,
        status = toTransactionStatus(),
        type = TxInfo.TransactionType.Swap,
        amount = viewedAmount,
    )
}

/**
 * Maps the typed express status to the on-chain-shaped [TxInfo.TransactionStatus] used by the UI:
 * the single success state (`Finished`) → Confirmed, any other terminal state → Failed, in-progress → Unconfirmed.
 */
private fun ExpressTx.toTransactionStatus(): TxInfo.TransactionStatus {
    val isFinished = when (this) {
        is ExpressTx.Swap -> tx.status == ExpressExchangeStatus.Finished
        is ExpressTx.Onramp -> tx.status == ExpressOnrampStatus.Finished
    }
    return when {
        isFinished -> TxInfo.TransactionStatus.Confirmed
        isTerminal -> TxInfo.TransactionStatus.Failed
        else -> TxInfo.TransactionStatus.Unconfirmed
    }
}