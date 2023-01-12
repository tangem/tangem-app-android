package com.tangem.feature.swap.domain.models

/**
 * Quote model holds data about current amounts of exchange and fees
 *
 * @property fromTokenAmount amount of token you want to exchange
 * @property toTokenAmount amount of token you want to receive
 * @property fromTokenAddress address token you want to exchange
 * @property toTokenAddress address token you want to receive
 * @property estimatedGas fee
 * @property isAllowedToSpend does spend allowed
 */
data class QuoteModel(
    val fromTokenAmount: SwapAmount,
    val toTokenAmount: SwapAmount,
    val fromTokenAddress: String,
    val toTokenAddress: String,
    val estimatedGas: Int,
    val isAllowedToSpend: Boolean = false,
)
