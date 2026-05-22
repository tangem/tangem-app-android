package com.tangem.feature.swap.domain.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.models.ui.FeeBucket
import com.tangem.feature.swap.domain.models.ui.SwapFee
import java.math.BigDecimal

/**
 * Builds [SwapFee] instances from raw [TransactionFeeResult] payloads.
 *
 * [REDACTED_TASK_KEY] — Phase 3. Keeps the bucket-selection rules in one place so that
 * `SwapInteractor.loadSwapFee` (DEX path, CEX path) and `applySwapFee` (added in Phase 4) stay
 * in sync.
 *
 * Bucket selection mirrors the rules the send-v2 `FeeItemConverter` uses to populate the fee
 * selector list (`TransactionFee.Choosable` → Slow/Market/Fast; `TransactionFee.Single` →
 * Market). When the caller explicitly asks for a tier other than the default `MARKET`, the
 * matching [Fee] is sourced from the [TransactionFee] payload; otherwise `MARKET` is the
 * default since every variant exposes a `normal` field.
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
     *   unless the provider is DEX_BRIDGE.
     * @param feeBucket the tier to use; defaults to [FeeBucket.MARKET]. The selected
     *   [SwapFee.fee] is sourced from the [TransactionFee] shape accordingly.
     */
    fun fromLoaded(
        transactionFeeResult: TransactionFeeResult.Loaded,
        selectedFeeToken: CryptoCurrencyStatus,
        otherNativeFee: BigDecimal = BigDecimal.ZERO,
        feeBucket: FeeBucket = FeeBucket.MARKET,
    ): SwapFee = SwapFee(
        fee = selectFee(transactionFeeResult.fee, feeBucket),
        transactionFeeResult = transactionFeeResult,
        selectedFeeToken = selectedFeeToken,
        otherNativeFee = otherNativeFee,
        feeBucket = feeBucket,
    )

    /**
     * Builds a [SwapFee] from a [TransactionFeeResult.LoadedExtended] (gasless / token-fee
     * branch).
     *
     * `LoadedExtended` always carries a single [TransactionFeeExtended.transactionFee] (no
     * slow/normal/priority choice), so the bucket defaults to [FeeBucket.MARKET].
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
}