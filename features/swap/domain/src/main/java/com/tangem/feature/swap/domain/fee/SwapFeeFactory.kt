package com.tangem.feature.swap.domain.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.models.ui.FeeBucket
import com.tangem.feature.swap.domain.models.ui.SwapFee
import java.math.BigDecimal

/**
 * Builds [SwapFee] instances from raw [TransactionFeeResult] payloads.
 *
 * Keeps the bucket-selection rules in one place so that
 * `SwapInteractor.loadSwapFee` (DEX path, CEX path) and `applySwapFee` (added in Phase 4) stay
 * in sync.
 *
 * Bucket selection mirrors the rules the send-v2 `FeeItemConverter` uses to populate the fee
 * selector list (`TransactionFee.Choosable` → Slow/Market/Fast; `TransactionFee.Single` →
 * Market). When the caller explicitly asks for a tier other than the default `MARKET`, the
 * matching [Fee] is sourced from the [TransactionFee] payload; otherwise `MARKET` is the
 * default since every variant exposes a `normal` field.
 *
 * ## Bridge-fee folding
 *
 * For DEX_BRIDGE providers the express quote carries a native-coin bridge protocol fee
 * (`otherNativeFee`) that the wallet pays on top of the network gas fee — both leave the wallet
 * in the **native coin**. To make the single "Network fee" shown to the user reflect the true
 * native cost, this factory folds `otherNativeFee` into every tier of the produced
 * [Fee] / [TransactionFee] — but only when the fee is paid in the native coin. After folding:
 *  - [SwapFee.fee].amount.value = gas + bridge — the single source of truth used for display,
 *    balance/validation, the success screen and the fee-coverage warning;
 *  - [SwapFee.otherNativeFee] = the bridge portion **already included** in `fee.amount`, retained
 *    only so a gas-only figure can be recovered (`fee.amount - otherNativeFee`, e.g. the
 *    "high network fee" check). It is never added again downstream.
 *
 * The fold is skipped for token-denominated (gasless) fees, because a native-coin addend cannot
 * be summed into a token fee — see [fromLoadedExtended].
 */
object SwapFeeFactory {

    /**
     * Builds a [SwapFee] from a [TransactionFeeResult.Loaded] (native-fee branch).
     *
     * @param transactionFeeResult the raw fee payload — its `.fee` is the [TransactionFee] that
     *   determines the available buckets.
     * @param selectedFeeToken the currency that pays the fee. For native fee paths this is the
     *   native coin status of the from-token's network.
     * @param otherNativeFee bridge protocol fee from `DexFeeResult.otherNativeFee`. Zero
     *   unless the provider is DEX_BRIDGE. When positive and the fee is native, it is folded into
     *   `fee`/`transactionFeeResult` (see class docs) so the displayed fee is the gas+bridge total.
     * @param feeBucket the tier to use; defaults to [FeeBucket.MARKET]. The selected
     *   [SwapFee.fee] is sourced from the (folded) [TransactionFee] shape accordingly.
     */
    fun fromLoaded(
        transactionFeeResult: TransactionFeeResult.Loaded,
        selectedFeeToken: CryptoCurrencyStatus,
        otherNativeFee: BigDecimal = BigDecimal.ZERO,
        feeBucket: FeeBucket = FeeBucket.MARKET,
    ): SwapFee {
        val foldedFee = transactionFeeResult.fee.foldNativeFee(selectedFeeToken, otherNativeFee)
        val foldedResult =
            if (foldedFee === transactionFeeResult.fee) transactionFeeResult else TransactionFeeResult.Loaded(foldedFee)
        return SwapFee(
            fee = selectFee(foldedResult.fee, feeBucket),
            transactionFeeResult = foldedResult,
            selectedFeeToken = selectedFeeToken,
            otherNativeFee = otherNativeFee,
            feeBucket = feeBucket,
        )
    }

    /**
     * Builds a [SwapFee] from a [TransactionFeeResult.LoadedExtended] (gasless / token-fee
     * branch).
     *
     * `LoadedExtended` always carries a single [TransactionFeeExtended.transactionFee] (no
     * slow/normal/priority choice), so the bucket defaults to [FeeBucket.MARKET].
     *
     * The native-coin `otherNativeFee` is **not** folded here: a gasless fee is
     * token-denominated, and a native addend cannot be summed into a token fee (different
     * currency). DEX gasless is currently unreachable (`SwapModel.loadFeeExtended` returns a
     * `GaslessError` for DEX/DEX_BRIDGE), so this branch never carries a non-zero `otherNativeFee`
     * today — the guard below is defensive.
     *
     * TODO [REDACTED_TASK_KEY]: when DEX gasless support lands, surface/charge the native `otherNativeFee`
     *  separately here instead of folding it into the token gas fee. Tie-in with the
     *  "TODO support gasless in DEX/DEX_BRIDGE" note in `SwapModel.loadFeeExtended`.
     */
    fun fromLoadedExtended(
        transactionFeeResult: TransactionFeeResult.LoadedExtended,
        selectedFeeToken: CryptoCurrencyStatus,
        otherNativeFee: BigDecimal = BigDecimal.ZERO,
        feeBucket: FeeBucket = FeeBucket.MARKET,
    ): SwapFee = SwapFee(
        fee = selectFee(transactionFeeResult.fee.transactionFee, feeBucket),
        transactionFeeResult = transactionFeeResult,
        selectedFeeToken = selectedFeeToken,
        otherNativeFee = otherNativeFee,
        feeBucket = feeBucket,
    )

    /**
     * Convenience entry-point that picks the right [fromLoaded] / [fromLoadedExtended] variant
     * automatically.
     */
    fun from(
        transactionFeeResult: TransactionFeeResult,
        selectedFeeToken: CryptoCurrencyStatus,
        otherNativeFee: BigDecimal = BigDecimal.ZERO,
        feeBucket: FeeBucket = FeeBucket.MARKET,
    ): SwapFee = when (transactionFeeResult) {
        is TransactionFeeResult.Loaded -> fromLoaded(
            transactionFeeResult = transactionFeeResult,
            selectedFeeToken = selectedFeeToken,
            otherNativeFee = otherNativeFee,
            feeBucket = feeBucket,
        )
        is TransactionFeeResult.LoadedExtended -> fromLoadedExtended(
            transactionFeeResult = transactionFeeResult,
            selectedFeeToken = selectedFeeToken,
            otherNativeFee = otherNativeFee,
            feeBucket = feeBucket,
        )
    }

    /**
     * Selects the concrete [Fee] from a [TransactionFee] for a given [FeeBucket].
     *
     * Falls back to [TransactionFee.normal] when the requested bucket is unavailable on the
     * payload — this happens, for example, when [FeeBucket.SLOW] is asked for on a
     * [TransactionFee.Single] (which only has `normal`). Matches the behaviour of
     * `FeeItemConverter.addFeeItemsFull`, which silently degrades a `Choosable`-only bucket to
     * `Market` when the payload is `Single`.
     *
     * [FeeBucket.SUGGESTED] and [FeeBucket.CUSTOM] are not available from a plain
     * [TransactionFee] (Suggested comes from `FeeStateConfiguration.Suggestion.fee`; Custom is
     * user-edited). For both we fall back to `normal`; the caller is expected to override
     * [SwapFee.fee] with the suggestion / custom fee when applicable.
     */
    private fun selectFee(transactionFee: TransactionFee, feeBucket: FeeBucket): Fee = when (transactionFee) {
        is TransactionFee.Choosable -> when (feeBucket) {
            FeeBucket.SLOW -> transactionFee.minimum
            FeeBucket.MARKET -> transactionFee.normal
            FeeBucket.FAST -> transactionFee.priority
            FeeBucket.SUGGESTED,
            FeeBucket.CUSTOM,
            -> transactionFee.normal
        }
        is TransactionFee.Single -> transactionFee.normal
    }

    // region [REDACTED_TASK_KEY] — bridge-fee folding

    /**
     * Folds a native-coin [otherNativeFee] into every tier of this [TransactionFee], but only
     * when the fee is paid in the native coin (`selectedFeeToken` is a [CryptoCurrency.Coin]) and
     * the amount is positive. Returns `this` unchanged otherwise — identity is preserved so
     * non-bridge swaps produce a byte-identical result.
     */
    private fun TransactionFee.foldNativeFee(
        selectedFeeToken: CryptoCurrencyStatus,
        otherNativeFee: BigDecimal,
    ): TransactionFee {
        val canFold = otherNativeFee.signum() > 0 && selectedFeeToken.currency is CryptoCurrency.Coin
        return if (canFold) plusNativeFee(otherNativeFee) else this
    }

    /** Adds [delta] to the `amount` of every tier of this [TransactionFee]. */
    private fun TransactionFee.plusNativeFee(delta: BigDecimal): TransactionFee = when (this) {
        is TransactionFee.Choosable -> copy(
            minimum = minimum.plusNativeAmount(delta),
            normal = normal.plusNativeAmount(delta),
            priority = priority.plusNativeAmount(delta),
        )
        is TransactionFee.Single -> copy(normal = normal.plusNativeAmount(delta))
    }

    /**
     * Returns a copy of this [Fee] with [delta] added to `amount.value`, preserving every other
     * field (gasLimit/gasPrice etc. stay intact, so signing is unaffected). No-op when [delta] is
     * zero (identity preserved). Token-denominated fees ([Fee.Ethereum.TokenCurrency],
     * [Fee.CardanoToken]) are returned unchanged — a native-coin addend must never be summed into
     * a token fee. The `when` is exhaustive so a new SDK [Fee] subtype fails compilation rather
     * than silently skipping the fold. High cyclomatic complexity is inherent to that exhaustive
     * per-subtype `copy` dispatch (no branching logic), so the rule is suppressed here.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun Fee.plusNativeAmount(delta: BigDecimal): Fee {
        if (delta.signum() == 0) return this
        val newAmount = amount.copy(value = (amount.value ?: BigDecimal.ZERO) + delta)
        return when (this) {
            // Token-denominated fees — a native-coin addend must never be summed in.
            is Fee.Ethereum.TokenCurrency,
            is Fee.CardanoToken,
            -> this
            is Fee.Ethereum.Legacy -> copy(amount = newAmount)
            is Fee.Ethereum.EIP1559 -> copy(amount = newAmount)
            is Fee.Alephium -> copy(amount = newAmount)
            is Fee.Aptos -> copy(amount = newAmount)
            is Fee.Bitcoin -> copy(amount = newAmount)
            is Fee.Common -> copy(amount = newAmount)
            is Fee.Filecoin -> copy(amount = newAmount)
            is Fee.Hedera -> copy(amount = newAmount)
            is Fee.Kaspa -> copy(amount = newAmount)
            is Fee.Sui -> copy(amount = newAmount)
            is Fee.Tron -> copy(amount = newAmount)
            is Fee.VeChain -> copy(amount = newAmount)
        }
    }

    // endregion
}