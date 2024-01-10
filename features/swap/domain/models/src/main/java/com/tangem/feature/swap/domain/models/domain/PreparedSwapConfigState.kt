package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount

/**
 * Prepared swap config state that contains flags to determine
 *
 * @property isAllowedToSpend shows is token allowed to spend
 * @property isBalanceEnough shows is balance of token enough
 * @property isFeeEnough shows is amount of main coin enough for fee
 */
// todo Refactor this state
data class PreparedSwapConfigState(
    val isAllowedToSpend: Boolean,
    val isBalanceEnough: Boolean,
    val isFeeEnough: Boolean,
    val hasOutgoingTransaction: Boolean,
    val includeFeeInAmount: IncludeFeeInAmount,
)

sealed class IncludeFeeInAmount {
    data class Included(val amountSubtractFee: SwapAmount) : IncludeFeeInAmount()
    object Excluded : IncludeFeeInAmount()
    object BalanceNotEnough : IncludeFeeInAmount()
}