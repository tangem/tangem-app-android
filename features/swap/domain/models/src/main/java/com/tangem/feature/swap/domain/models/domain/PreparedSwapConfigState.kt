package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount

/**
 * Prepared swap config state that contains flags to determine
 *
 * @property isAllowedToSpend shows is token allowed to spend
 * @property isBalanceEnough shows is balance of token enough
 */
// todo Refactor this state
data class PreparedSwapConfigState(
    val isAllowedToSpend: Boolean,
    val isBalanceEnough: Boolean,
    val feeState: SwapFeeState,
    val hasOutgoingTransaction: Boolean,
    val includeFeeInAmount: IncludeFeeInAmount,
)

sealed class IncludeFeeInAmount {
    data class Included(val amountSubtractFee: SwapAmount) : IncludeFeeInAmount()
    data object Excluded : IncludeFeeInAmount()
    data object BalanceNotEnough : IncludeFeeInAmount()
}