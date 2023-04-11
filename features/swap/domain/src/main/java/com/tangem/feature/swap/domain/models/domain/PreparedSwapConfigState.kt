package com.tangem.feature.swap.domain.models.domain

/**
 * Prepared swap config state that contains flags to determine
 *
 * @property isAllowedToSpend shows is token allowed to spend
 * @property isBalanceEnough shows is balance of token enough
 * @property isFeeEnough shows is amount of main coin enough for fee
 */
data class PreparedSwapConfigState(
    val isAllowedToSpend: Boolean,
    val isBalanceEnough: Boolean,
    val isFeeEnough: Boolean,
)
