package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount

/**
 * Prepared swap config state derived from the resolved fee.
 *
 * Populated by [SwapInteractor.applySwapFee] after the fee selector
 * emits a `FeeSelectorUM.Content` state. Until then the quote carries a transient
 * [SwapBalanceStatus.Pending]. Consumers must therefore not derive UI decisions from
 * [balanceStatus] before the fee has resolved (see [SwapBalanceStatus.Pending]).
 *
 * @property balanceStatus unified balance-vs-fee comparison result that drives UI decisions
 *   (swap-button enabled, InsufficientFunds card, UnableToCoverFee warning, FeeCoverage warning).
 * @property hasOutgoingTransaction whether the source currency has a pending outgoing transaction.
 */
data class PreparedSwapConfigState(
    val balanceStatus: SwapBalanceStatus,
    val hasOutgoingTransaction: Boolean,
)

/**
 * Unified balance + fee check result for a swap.
 */
sealed interface SwapBalanceStatus {

    /** Fee not yet resolved. DEX returns this from `loadDexSwapDataNoFee`. */
    data object Pending : SwapBalanceStatus

    /** Balance covers amount + fee. Fee currency balance covers fee. */
    data object Sufficient : SwapBalanceStatus

    /**
     * CEX only: amount fits, fee does not, but amount can be reduced by `feeAmount` so the
     * fee fits within the from-token balance. [adjustedAmount] is consumed by `manageCex`
     * before calling `repository.findBestQuote` (the requote uses the reduced amount). It is
     * also surfaced into the `FeeCoverageNotification` and into `manageWarnings` /
     * `getCoinBalanceAfterTransaction` so the existential-deposit / dust / reserve checks see
     * the reduced amount.
     */
    data class FeeAdjustedAmount(val adjustedAmount: SwapAmount) : SwapBalanceStatus

    /**
     * Amount itself exceeds balance. Disables the swap button and drives the
     * `InsufficientFunds` card in `StateBuilder.isInsufficientFundsCondition`.
     */
    data object InsufficientAmount : SwapBalanceStatus

    /**
     * Amount fits, but the fee currency balance is below the fee. Drives the
     * `UnableToCoverFeeWarning` notification. Carries the fee currency name and symbol so the
     * warning can name the missing currency (e.g. "Not enough ETH for fee").
     */
    data class InsufficientFee(
        val feeCurrencyName: String?,
        val feeCurrencySymbol: String?,
    ) : SwapBalanceStatus
}