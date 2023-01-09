package com.tangem.feature.swap.domain.models

/**
 * Swap transaction model
 *
 * @property fromTokenAddress token from which want to convert
 * @property toTokenAddress token to want to convert
 * @property toTokenAmount amount "target" token
 * @property fromTokenAmount amount "initial" token
 * @property fromWalletAddress wallet from address (transactions will be sent from this address)
 * @property toWalletAddress transactions will be sent to our(1inch) contract address
 * @property data The encoded data to call the approve method on the swapped token contract
 * @property value  Native token value in WEI (for approve is always 0)
 * @property gasPrice maximum amount of gas for a swap default: 11500000; max: 11500000
 * @property gas estimated amount of the gas limit, increase this value by 25%
 * @constructor Create empty Swap transaction model
 */
data class SwapTransactionModel(
    val fromTokenAddress: String,
    val toTokenAddress: String,
    val toTokenAmount: String,
    val fromTokenAmount: String,
    val fromWalletAddress: String,
    val toWalletAddress: String,
    val data: String,
    val value: String,
    val gasPrice: String,
    val gas: String,
)
