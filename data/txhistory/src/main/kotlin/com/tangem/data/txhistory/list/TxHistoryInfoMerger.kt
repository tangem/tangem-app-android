package com.tangem.data.txhistory.list

import com.tangem.blockchain.common.isUTXO
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.OnrampTransaction
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.model.explorerHash
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Merges the on-chain pagination backbone with the express (swap/onramp) overlay into a single
 * timestamp-DESC timeline.
 *
 * Per express op:
 *  1. deterministic hash match (its [ExpressTx.matchHash] — `payin_hash`/`payout_hash` — against an on-chain hash);
 *  2. if that fails, a [heuristic fallback][matchesOnChainHeuristically] by direction + address + amount + time,
 *     scoped to the open [currency].
 *
 * The heuristic exists because the on-chain hash is not always returned by the provider — the payout leg's hash
 * is filled by status polling and may never arrive. Without it an incoming swap/onramp would surface twice: once
 * as the standalone express row and once as its BSDK leg. Matching them collapses the pair into one enriched row.
 *
 * Per outcome:
 *  - matched   → enrich: emit the express row carrying its on-chain leg; the on-chain tx(es) of that hash are
 *                collapsed into this row (not emitted standalone).
 *  - unmatched → standalone row (status shown, no on-chain leg), except a terminal-but-unsuccessful incoming swap
 *                leg whose on-chain credit never arrived, which is hidden (see [isHiddenWhenUnmatched]) so it does not
 *                surface as a phantom incoming row. A successful (finished) incoming swap, the outgoing / refund side,
 *                and every onramp purchase are always kept so the user still sees their deals.
 *
 * On-chain transactions that no express op claimed pass through as [OnChainTx].
 * [onChain] is expected to be already de-duplicated (by `identityKey`) by the caller.
 *
 * @param currency the open currency: its [ExpressAsset.ID] scopes the heuristics to legs of this token (network +
 *  contract — replacing the per-leg token/network check, since [TxInfo] carries no network of its own), and its
 *  chain decides whether the pay-in amount tolerates a small UTXO fee/dust variance or must match exactly.
 */
internal fun mergeTxHistoryInfos(
    onChain: List<TxInfo>,
    express: List<ExpressTx>,
    currency: CryptoCurrency,
): List<TxHistoryInfo> {
    val currencyAssetId: ExpressAsset.ID = ExpressAsset.ID(currency)
    val isUtxoNetwork: Boolean = currency.network.toBlockchain().isUTXO
    val onChainByHash = onChain.associateBy { it.txHash }
    val claimedHashes = mutableSetOf<String>()
    val result = mutableListOf<TxHistoryInfo>()

    // Phase 1 — deterministic hash match takes priority (payin_hash / payout_hash via ExpressTx.matchHash).
    val unmatched = mutableListOf<ExpressTx>()
    express.forEach { op ->
        val byHash = op.matchHash?.let(onChainByHash::get)
        if (byHash != null && byHash.txHash !in claimedHashes) {
            result += op.withMatchedOnChain(OnChainTx.BSDK(byHash))
            claimedHashes += byHash.txHash
        } else {
            unmatched += op
        }
    }

    // Phase 2 — heuristic fallback for ops whose on-chain hash the provider never returned. On ties, the on-chain
    // tx closest in time to the deal creation wins (collision resolution). An op that still finds no leg is emitted
    // standalone, unless it must be hidden (a terminal swap payout that never landed on-chain).
    unmatched.forEach { op ->
        val candidate = onChain
            .filter { tx ->
                tx.txHash !in claimedHashes && op.matchesOnChainHeuristically(tx, currencyAssetId, isUtxoNetwork)
            }
            .minByOrNull { abs(it.timestampInMillis - op.createdAtMillis) }
        when {
            candidate != null -> {
                result += op.withMatchedOnChain(OnChainTx.BSDK(candidate))
                claimedHashes += candidate.txHash
            }
            !op.isHiddenWhenUnmatched() -> result += op
        }
    }

    // Phase 3 — on-chain txs no express op claimed pass through.
    onChain.forEach { tx ->
        if (tx.txHash !in claimedHashes) result += OnChainTx.BSDK(tx)
    }

    return result.sortedByDescending(TxHistoryInfo::timestampMillis)
}

/**
 * TangemPay counterpart of [mergeTxHistoryInfos]: merges the TangemPay on-chain backbone with the
 * express overlay. Hash-based match only — the heuristic fallback is BSDK-specific (it relies on
 * [TxInfo] direction/addresses/amount) and TangemPay is not wired into the history end-to-end yet.
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

/**
 * Whether an express op that matched no on-chain leg must be hidden instead of surfacing standalone.
 *
 * Only a *terminal-but-unsuccessful* incoming (payout) leg of a swap is hidden — one that ended in failure / refund /
 * expiry without ever landing on-chain (or landing somewhere we do not page); a standalone "You receive" row there
 * would be a phantom. A *successful* (finished) incoming swap is always kept even unmatched: the credit did arrive
 * and must be shown whether or not we could join it to an on-chain tx — the deciding case for the index-table
 * backbone, where there is no on-chain row to fall back to. The outgoing / refund side of a swap
 * ([ExpressTx.Swap.isOutgoing], viewed on the from-token screen — where a refund lands) and every onramp purchase are
 * always kept too.
 */
private fun ExpressTx.isHiddenWhenUnmatched(): Boolean = when (this) {
    is ExpressTx.Swap -> isTerminal && !tx.status.isFinished && !isOutgoing
    is ExpressTx.Onramp -> false
}

/**
 * Does the on-chain [onChainTx] look like the leg of this express op, when their hashes did not line up?
 *
 * Dispatches by op kind and viewed direction. The matched leg's asset must be the open currency ([currencyAssetId]) —
 * this replaces the per-leg `T.network == E.*_network && T.contract == E.*_contract` check ([TxInfo] itself carries
 * no network):
 *  - outgoing swap → the pay-in leg it sent;
 *  - refunded outgoing swap → the incoming refund of the `from` asset that lands on the same screen;
 *  - incoming swap → the payout leg it received (the case the missing payout hash breaks);
 *  - onramp → the payout leg it received.
 */
private fun ExpressTx.matchesOnChainHeuristically(
    onChainTx: TxInfo,
    currencyAssetId: ExpressAsset.ID,
    isUtxoNetwork: Boolean,
): Boolean = when (this) {
    is ExpressTx.Swap -> when {
        isOutgoing && !onChainTx.isOutgoing && tx.status == ExpressExchangeStatus.Refunded ->
            tx.matchesRefund(onChainTx, currencyAssetId)
        isOutgoing -> tx.matchesOutgoingPayin(onChainTx, currencyAssetId, isUtxoNetwork)
        else -> tx.matchesIncomingPayout(onChainTx, currencyAssetId)
    }
    is ExpressTx.Onramp -> tx.matchesOnrampPayout(onChainTx, currencyAssetId)
}

/**
 * Outgoing (pay-in) leg: the user sent the `from` asset to the provider deposit address. That address
 * ([ExchangeTransaction.payinAddress]) is a per-deal discriminator; the sender and amount confirm it. The amount is
 * matched exactly, allowing the small UTXO fee/dust variance ([OUTGOING_AMOUNT_TOLERANCE]) only on UTXO chains.
 */
private fun ExchangeTransaction.matchesOutgoingPayin(
    onChainTx: TxInfo,
    currencyAssetId: ExpressAsset.ID,
    isUtxoNetwork: Boolean,
): Boolean {
    if (!onChainTx.isOutgoing) return false
    if (!fromAsset.id.matchesAssetId(currencyAssetId)) return false
    if (onChainTx.destinationAddresses().none { it.matchesAddress(payinAddress) }) return false
    if (!isSentFromUser(onChainTx)) return false
    val tolerance = if (isUtxoNetwork) OUTGOING_AMOUNT_TOLERANCE else EXACT_AMOUNT_TOLERANCE
    return onChainTx.amount.matchesWithin(fromAmount, tolerance)
}

/**
 * Incoming (payout) leg: the `to` asset landed on the user's payout address. This is where the missing
 * payout hash bites, so address + amount + a 24h window carry the match; the sender must not be the user
 * (self-transfer exclusion). Amount matches the actual settled value exactly when known
 * ([ExchangeTransaction.toActualAmount]), otherwise the expected value within the slippage tolerance.
 */
private fun ExchangeTransaction.matchesIncomingPayout(onChainTx: TxInfo, currencyAssetId: ExpressAsset.ID): Boolean {
    if (onChainTx.isOutgoing) return false
    if (!toAsset.id.matchesAssetId(currencyAssetId)) return false
    if (onChainTx.destinationAddresses().none { it.matchesAddress(payoutAddress) }) return false
    if (isSentFromUser(onChainTx)) return false // self-transfer, not a provider payout
    if (!isWithinWindow(from = createdAtMillis, upperBase = createdAtMillis, ts = onChainTx.timestampInMillis)) {
        return false
    }
    return matchesIncomingAmount(onChainTx.amount, actual = toActualAmount, expected = toAmount)
}

/**
 * Refund leg: a refunded swap returns the `from` asset to the user as an incoming tx on the from-token
 * screen. Matched by direction + status + amount (within a wider tolerance for fees/volatility) + a window
 * that extends to `updatedAt + 24h`, since the refund is credited after the deal was last updated. When the refund
 * asset is known ([ExchangeTransaction.refundAssetId]) it must be the open currency.
 */
private fun ExchangeTransaction.matchesRefund(onChainTx: TxInfo, currencyAssetId: ExpressAsset.ID): Boolean {
    if (onChainTx.isOutgoing) return false
    val refundId = refundAssetId
    if (refundId != null && !refundId.matchesAssetId(currencyAssetId)) return false
    if (isSentFromUser(onChainTx)) return false // refund comes from the provider, not the user
    if (!isWithinWindow(from = createdAtMillis, upperBase = updatedAtMillis, ts = onChainTx.timestampInMillis)) {
        return false
    }
    return onChainTx.amount.matchesWithin(fromAmount, REFUND_AMOUNT_TOLERANCE)
}

/**
 * Onramp payout leg: bought crypto landed on the user's payout address. No `from` address to exclude
 * (the source is fiat), so address + amount + the 24h window carry the match.
 */
private fun OnrampTransaction.matchesOnrampPayout(onChainTx: TxInfo, currencyAssetId: ExpressAsset.ID): Boolean {
    if (onChainTx.isOutgoing) return false
    if (!toAsset.id.matchesAssetId(currencyAssetId)) return false
    if (onChainTx.destinationAddresses().none { it.matchesAddress(payoutAddress) }) return false
    if (!isWithinWindow(from = createdAtMillis, upperBase = createdAtMillis, ts = onChainTx.timestampInMillis)) {
        return false
    }
    return matchesIncomingAmount(onChainTx.amount, actual = toActualAmount, expected = toAmount)
}

/**
 * Amount match for an incoming leg: the [actual] settled amount matched exactly when known, otherwise the
 * [expected] amount within [INCOMING_SLIPPAGE_TOLERANCE]; no match when neither is known.
 */
private fun matchesIncomingAmount(txAmount: BigDecimal, actual: BigDecimal?, expected: BigDecimal?): Boolean = when {
    actual != null -> txAmount.matchesWithin(actual, EXACT_AMOUNT_TOLERANCE)
    expected != null -> txAmount.matchesWithin(expected, INCOMING_SLIPPAGE_TOLERANCE)
    else -> false
}

/** Whether this asset id is the open currency's asset (network id + contract), tolerant of EVM contract casing. */
private fun ExpressAsset.ID.matchesAssetId(other: ExpressAsset.ID): Boolean =
    networkId.equals(other.networkId, ignoreCase = true) &&
        contractAddress.equals(other.contractAddress, ignoreCase = true)

/** Relative-difference amount comparison: `|this - target| / |target| <= tolerance`. */
private fun BigDecimal.matchesWithin(target: BigDecimal, tolerance: BigDecimal): Boolean {
    if (target.signum() == 0) return signum() == 0
    val relativeDiff = (this - target).abs().divide(target.abs(), AMOUNT_SCALE, RoundingMode.HALF_UP)
    return relativeDiff <= tolerance
}

/** Inclusive time window `[from, upperBase + 24h]`. */
private fun isWithinWindow(from: Long, upperBase: Long, ts: Long): Boolean {
    val upperBound = upperBase + TIME_WINDOW_MILLIS
    return ts in from..upperBound
}

/**
 * Whether the on-chain tx was sent from the user's own `from` address — i.e. a self-transfer, not a
 * provider payout/refund.
 */
private fun ExchangeTransaction.isSentFromUser(onChainTx: TxInfo): Boolean {
    val from = fromAddress
    return onChainTx.sourceAddresses().any { it.matchesAddress(from) }
}

/**
 * Address equality tolerant of case: EVM addresses differ only by checksum casing, and other chains'
 * distinct addresses won't collide case-insensitively in practice.
 */
private fun String.matchesAddress(other: String): Boolean = equals(other, ignoreCase = true)

private fun TxInfo.sourceAddresses(): List<String> = when (val source = sourceType) {
    is TxInfo.SourceType.Single -> listOf(source.address)
    is TxInfo.SourceType.Multiple -> source.addresses
}

private fun TxInfo.destinationAddresses(): List<String> = when (val destination = destinationType) {
    is TxInfo.DestinationType.Single -> listOf(destination.addressType.address)
    is TxInfo.DestinationType.Multiple -> destination.addressTypes.map { it.address }
}

/** Scale used when computing relative amount differences. */
private const val AMOUNT_SCALE = 10

/** 24h credit/refund buffer for the incoming and refund time windows. */
private const val TIME_WINDOW_MILLIS = 86_400_000L

/** 0% — exact match, used for the pay-in amount off UTXO chains and for the confirmed actual settled amount. */
private val EXACT_AMOUNT_TOLERANCE = BigDecimal.ZERO

/** 0.1% — absorbs UTXO fee/dust variance on the pay-in amount (applied only on UTXO chains). */
private val OUTGOING_AMOUNT_TOLERANCE = BigDecimal("0.001")

/** 5% — payout slippage vs the expected `to` amount (used only when no actual amount is known). */
private val INCOMING_SLIPPAGE_TOLERANCE = BigDecimal("0.05")

/** 15% — fees + volatility on the refunded amount vs the original `from` amount. */
private val REFUND_AMOUNT_TOLERANCE = BigDecimal("0.15")