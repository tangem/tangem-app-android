package com.tangem.feature.swap.domain.models.data

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
    val toTokenAmount: String,
    val fromTokenAmount: String,
    val transaction: TransactionModel
)
