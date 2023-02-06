package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount

/**
 * Swap transaction model
 *
 * @property fromTokenAddress token from which want to convert
 * @property toTokenAddress token to want to convert
 * @property toTokenAmount amount "target" token
 * @property fromTokenAmount amount "initial" token
 */
data class SwapDataModel(
    val fromTokenAddress: String,
    val toTokenAddress: String,
    val toTokenAmount: SwapAmount,
    val fromTokenAmount: SwapAmount,
    val transaction: TransactionModel,
)
